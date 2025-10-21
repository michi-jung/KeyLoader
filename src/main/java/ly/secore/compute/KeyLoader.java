package ly.secore.compute;

import ly.secore.compute.ComputeDevice;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.HexFormat;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import iaik.pkcs.pkcs11.wrapper.CK_ATTRIBUTE;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM;
import iaik.pkcs.pkcs11.wrapper.PKCS11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Connector;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;

public class KeyLoader implements AutoCloseable {
  protected PKCS11 p11;
  long hSession;

  public final static int KEY_AGREEMENT_RANDOM_LEN       = 32;
  public final static int SECP256R1_PUBLIC_KEY_INFO_LEN  = 91;
  public final static int SECP256R1_PUBLIC_KEY_LEN       = 67;
  public final static int SECP256R1_SIGNATURE_LEN        = 64;

  public static class KeyAgreementParameters {
    public byte[] initiatorRandom;
    public byte[] initiatorAuthPubKey;
    public byte[] responderRandom;
    public byte[] responderEphPubKey;
    public byte[] initiatorEphPubKey;
    public byte[] initiatorSignature;
  };

  protected final static byte[] SUBJECT_PUBLIC_KEY_INFO_PREFIX =
      HexFormat.of().parseHex("3059301306072A8648CE3D020106082A8648CE3D030107034200");

  public KeyLoader(String pkcs11ModuleFilename, long slotID)
      throws IOException, PKCS11Exception
  {
    p11 = PKCS11Connector.connectToPKCS11Module(pkcs11ModuleFilename);

    p11.C_Initialize(null, true);
    hSession = p11.C_OpenSession(slotID, PKCS11Constants.CKF_SERIAL_SESSION, 0, null);
  }

  public ComputeDevice.compute_device_mfg_reset_secret_s deriveMfgResetSecret(int derivationInput)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] mfgResetMasterKeyTemplate = new CK_ATTRIBUTE[3];
    CK_MECHANISM ckm_aes_ecb = new CK_MECHANISM();
    Memory memory;
    ComputeDevice.compute_device_mfg_reset_secret_s mfgResetSecret;
    byte[] cleartext;
    byte[] ciphertext;
    long[] hMasterKey;

    mfgResetMasterKeyTemplate[0] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    mfgResetMasterKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    mfgResetMasterKeyTemplate[1] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    mfgResetMasterKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    mfgResetMasterKeyTemplate[2] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[2].type = PKCS11Constants.CKA_LABEL;
    mfgResetMasterKeyTemplate[2].pValue = new String("MFG_RESET_MASTER");

    p11.C_FindObjectsInit(hSession, mfgResetMasterKeyTemplate, true);
    hMasterKey = p11.C_FindObjects(hSession, 1);
    p11.C_FindObjectsFinal(hSession);

    if (hMasterKey.length != 1) {
      throw new IOException("MFG_RESET_MASTER key not present.");
    }

    ckm_aes_ecb.mechanism = PKCS11Constants.CKM_AES_ECB;
    ckm_aes_ecb.pParameter = null;

    memory = new Memory(16);

    for (int offset = 0; offset < 16; offset += 4) {
      memory.setInt(offset, derivationInput);
    }

    cleartext = memory.getByteArray(0, 16);

    p11.C_EncryptInit(hSession, ckm_aes_ecb, hMasterKey[0], true);
    ciphertext = p11.C_Encrypt(hSession, null, cleartext);

    memory.write(0, ciphertext, 0, 16);
    mfgResetSecret = new ComputeDevice.compute_device_mfg_reset_secret_s(memory);

    return mfgResetSecret;
  }

  public KeyAgreementParameters keyAgreementStep1()
      throws IOException, PKCS11Exception
  {
    KeyAgreementParameters params;
    CK_ATTRIBUTE[] kldAuthKeyTemplate = new CK_ATTRIBUTE[3];
    CK_ATTRIBUTE[] publicKeyInfoAttr = new CK_ATTRIBUTE[1];
    long[] hKLDAuthKey;
    byte[] publicKeyInfo;

    kldAuthKeyTemplate[0] = new CK_ATTRIBUTE();
    kldAuthKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    kldAuthKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    kldAuthKeyTemplate[1] = new CK_ATTRIBUTE();
    kldAuthKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    kldAuthKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    kldAuthKeyTemplate[2] = new CK_ATTRIBUTE();
    kldAuthKeyTemplate[2].type = PKCS11Constants.CKA_LABEL;
    kldAuthKeyTemplate[2].pValue = new String("KLD_AUTH");

    p11.C_FindObjectsInit(hSession, kldAuthKeyTemplate, true);
    hKLDAuthKey = p11.C_FindObjects(hSession, 1);
    p11.C_FindObjectsFinal(hSession);

    if (hKLDAuthKey.length != 1) {
      throw new IOException("KLD_AUTH key not present.");
    }

    publicKeyInfoAttr[0] = new CK_ATTRIBUTE();
    publicKeyInfoAttr[0].type = PKCS11Constants.CKA_PUBLIC_KEY_INFO;

    p11.C_GetAttributeValue(hSession, hKLDAuthKey[0], publicKeyInfoAttr, true);

    publicKeyInfo = (byte [])publicKeyInfoAttr[0].pValue;

    if ((publicKeyInfo.length != SECP256R1_PUBLIC_KEY_INFO_LEN)        ||
        (publicKeyInfo[SUBJECT_PUBLIC_KEY_INFO_PREFIX.length] != 0x04) ||
        !Arrays.equals(Arrays.copyOfRange(publicKeyInfo, 0, SUBJECT_PUBLIC_KEY_INFO_PREFIX.length),
                       SUBJECT_PUBLIC_KEY_INFO_PREFIX))
    {
      throw new IOException("Malformed subjectPublicKeyInfo.");
    }

    params = new KeyAgreementParameters();
    params.initiatorAuthPubKey = new byte[SECP256R1_PUBLIC_KEY_LEN];
    params.initiatorAuthPubKey[0] = 0x04; /* OCTET STRING */
    params.initiatorAuthPubKey[1] = 0x41; /* Length: 65 Bytes */
    System.arraycopy(publicKeyInfo,
                     SUBJECT_PUBLIC_KEY_INFO_PREFIX.length,
                     params.initiatorAuthPubKey,
                     2,
                     65);

    params.initiatorRandom = new byte[KEY_AGREEMENT_RANDOM_LEN];

    p11.C_GenerateRandom(hSession, params.initiatorRandom);

    return params;
  }

  public void close() {
    try {
      p11.C_CloseSession(hSession);
      p11.C_Finalize(null);
    }
    catch (PKCS11Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
};
