/*
 * ly.secore.compute.HardwareSecurityModule
 * Load keys from an HSM to devices powered by compute secore.ly Firmware
 *
 * Copyright (c) 2025 secore.ly GmbH
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of secore.ly
 * GmbH ("Confidential Information").  You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with secore.ly GmbH or one of its
 * authorized partners.
 */

package ly.secore.compute;

import com.mythosil.sss4j.Share;
import com.mythosil.sss4j.Sss4j;
import com.sun.jna.Memory;
import iaik.pkcs.pkcs11.wrapper.CK_ATTRIBUTE;
import iaik.pkcs.pkcs11.wrapper.CK_ECDH1_DERIVE_PARAMS;
import iaik.pkcs.pkcs11.wrapper.CK_KEY_DERIVATION_STRING_DATA;
import iaik.pkcs.pkcs11.wrapper.CK_MECHANISM;
import iaik.pkcs.pkcs11.wrapper.PKCS11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Connector;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HardwareSecurityModule implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(HardwareSecurityModule.class);

  public final static int KEY_AGREEMENT_RANDOM_LEN       = 32;
  public final static int SECP256R1_PUBLIC_KEY_INFO_LEN  = 91;
  public final static int SECP256R1_PUBLIC_KEY_LEN       = 67;
  public final static int SECP256R1_SIGNATURE_LEN        = 64;
  public final static int AES_CMAC_LEN                   = 16;
  public final static int AES_256_KEY_LEN                = 32;

  public final static long CKA_X9_143_KBH      = 0x85EC0007L;
  public final static long CKM_X9_143_KEY_WRAP = 0x85EC0001L;

  public static class SetIncKeyContext {
    public byte[] initiatorRandom;
    public byte[] initiatorAuthPubKey;
    public byte[] responderRandom;
    public byte[] responderEphPubKey;
    public byte[] initiatorEphPubKey;
    public byte[] initiatorSignature;
    public byte[] responderCMAC;
    public byte[] initiatorCMAC;
    public byte[] initiatorKeyblock;

    public byte[] incKeyDerivationInfo;
    public long hInitiatorAuthPrivKey;
    public long hInitiatorEphPrivKey;
  };

  public static class RootHSMKeys {
    public String incarnationKeyKcv;
    public String firmwareValidationPublicKey;
    public String firmwareSigningPrivateKeyblock;
    public String hsmApplicationValidationPublicKeyblock;
    public String hsmApplicationSigningPrivateKeyblock;
    public String keyLoadingDeviceAuthenticationPublicKey;
    public String keyLoadingDeviceAuthenticationPrivateKeyblock;
    public String manufacturingResetMasterKeyblock;
  }

  public HardwareSecurityModule(String pkcs11ModuleFilename, long slotID)
      throws IOException, PKCS11Exception
  {
    p11 = PKCS11Connector.connectToPKCS11Module(pkcs11ModuleFilename);

    p11.C_Initialize(null, true);
    hSession = p11.C_OpenSession(slotID, PKCS11Constants.CKF_SERIAL_SESSION, 0, null);
  }

  public Device.ManufacturingResetSecret deriveMfgResetSecret(int derivationInput)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] mfgResetMasterKeyTemplate = new CK_ATTRIBUTE[3];
    CK_MECHANISM ckm_aes_ecb = new CK_MECHANISM();
    Memory memory;
    Device.ManufacturingResetSecret mfgResetSecret;
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
    mfgResetSecret = new Device.ManufacturingResetSecret(memory);

    return mfgResetSecret;
  }

  public SetIncKeyContext setIncKeyStep1(byte[] reincarnationKeyDerivationInfo)
      throws IOException, PKCS11Exception
  {
    SetIncKeyContext ctx;
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

    ctx = new SetIncKeyContext();
    ctx.incKeyDerivationInfo = reincarnationKeyDerivationInfo;
    ctx.initiatorAuthPubKey = new byte[SECP256R1_PUBLIC_KEY_LEN];
    ctx.initiatorAuthPubKey[0] = 0x04; /* OCTET STRING */
    ctx.initiatorAuthPubKey[1] = 0x41; /* Length: 65 Bytes */
    System.arraycopy(publicKeyInfo,
                     SUBJECT_PUBLIC_KEY_INFO_PREFIX.length,
                     ctx.initiatorAuthPubKey,
                     2,
                     65);
    ctx.initiatorRandom = new byte[KEY_AGREEMENT_RANDOM_LEN];
    p11.C_GenerateRandom(hSession, ctx.initiatorRandom);
    ctx.hInitiatorAuthPrivKey = hKLDAuthKey[0];

    return ctx;
  }

  public void setIncKeyStep2(SetIncKeyContext ctx)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] pubKeyTemplate = new CK_ATTRIBUTE[3];
    CK_ATTRIBUTE[] privKeyTemplate = new CK_ATTRIBUTE[3];
    CK_ATTRIBUTE[] ecPointAttr = new CK_ATTRIBUTE[1];
    CK_MECHANISM ckm_ecdsa_sha256 = new CK_MECHANISM();
    CK_MECHANISM ckm_ec_key_pair_gen = new CK_MECHANISM();
    long[] hKeys;

    pubKeyTemplate[0] = new CK_ATTRIBUTE();
    pubKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    pubKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    pubKeyTemplate[1] = new CK_ATTRIBUTE();
    pubKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    pubKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    pubKeyTemplate[2] = new CK_ATTRIBUTE();
    pubKeyTemplate[2].type = PKCS11Constants.CKA_EC_PARAMS;
    pubKeyTemplate[2].pValue = OID_SECP256R1;

    privKeyTemplate[0] = new CK_ATTRIBUTE();
    privKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    privKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    privKeyTemplate[1] = new CK_ATTRIBUTE();
    privKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    privKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    privKeyTemplate[2] = new CK_ATTRIBUTE();
    privKeyTemplate[2].type = PKCS11Constants.CKA_DERIVE;
    privKeyTemplate[2].pValue = true;

    ckm_ec_key_pair_gen.mechanism = PKCS11Constants.CKM_EC_KEY_PAIR_GEN;

    hKeys = p11.C_GenerateKeyPair(hSession,
                                  ckm_ec_key_pair_gen,
                                  pubKeyTemplate,
                                  privKeyTemplate,
                                  true);

    ecPointAttr[0] = new CK_ATTRIBUTE();
    ecPointAttr[0].type = PKCS11Constants.CKA_EC_POINT;

    p11.C_GetAttributeValue(hSession, hKeys[0], ecPointAttr, true);
    p11.C_DestroyObject(hSession, hKeys[0]);

    ctx.initiatorEphPubKey = (byte[])ecPointAttr[0].pValue;

    if (ctx.initiatorEphPubKey.length != SECP256R1_PUBLIC_KEY_LEN) {
      throw new IOException("Unexpected secp256r1 public key len");
    }

    ctx.hInitiatorEphPrivKey = hKeys[1];

    ckm_ecdsa_sha256.mechanism = PKCS11Constants.CKM_ECDSA_SHA256;

    p11.C_SignInit(hSession, ckm_ecdsa_sha256, ctx.hInitiatorAuthPrivKey, true);
    p11.C_SignUpdate(hSession, ctx.responderRandom);
    p11.C_SignUpdate(hSession, ctx.initiatorRandom);
    p11.C_SignUpdate(hSession, ctx.initiatorEphPubKey);
    ctx.initiatorSignature = p11.C_SignFinal(hSession);

    if (ctx.initiatorSignature.length != SECP256R1_SIGNATURE_LEN)
    {
      throw new IOException("Unexpected secp256r1 signature len");
    }
  }

 public void setIncKeyStep3(SetIncKeyContext ctx)
      throws IOException, PKCS11Exception
  {
    CK_ECDH1_DERIVE_PARAMS ecdh1_derive_params = new CK_ECDH1_DERIVE_PARAMS();
    CK_MECHANISM ckm_ecdh1_derive = new CK_MECHANISM();
    CK_MECHANISM ckm_extract_key_from_key = new CK_MECHANISM();
    CK_MECHANISM ckm_aes_cmac = new CK_MECHANISM();
    CK_MECHANISM ckm_x9_143_key_wrap = new CK_MECHANISM();
    CK_ATTRIBUTE[] masterSecretTemplate = new CK_ATTRIBUTE[4];
    CK_ATTRIBUTE[] responderMACKeyTemplate = new CK_ATTRIBUTE[4];
    CK_ATTRIBUTE[] initiatorMACKeyTemplate = new CK_ATTRIBUTE[4];
    CK_ATTRIBUTE[] ephKBPKTemplate = new CK_ATTRIBUTE[5];
    long extractParams;
    long masterSecretValueLen = 3 * AES_256_KEY_LEN;
    long macKeyValueLen = AES_256_KEY_LEN;
    long hMasterSecret;
    long hResponderMACKey;
    long hInitiatorMACKey;
    long hReincarnationKey;
    long hEphKBPKey;

    masterSecretTemplate[0] = new CK_ATTRIBUTE();
    masterSecretTemplate[0].type = PKCS11Constants.CKA_CLASS;
    masterSecretTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    masterSecretTemplate[1] = new CK_ATTRIBUTE();
    masterSecretTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    masterSecretTemplate[1].pValue = PKCS11Constants.CKK_GENERIC_SECRET;

    masterSecretTemplate[2] = new CK_ATTRIBUTE();
    masterSecretTemplate[2].type = PKCS11Constants.CKA_VALUE_LEN;
    masterSecretTemplate[2].pValue = masterSecretValueLen;

    masterSecretTemplate[3] = new CK_ATTRIBUTE();
    masterSecretTemplate[3].type = PKCS11Constants.CKA_DERIVE;
    masterSecretTemplate[3].pValue = true;

    ecdh1_derive_params.kdf = PKCS11Constants.CKD_SHA384_KDF;
    ecdh1_derive_params.pSharedData = new byte[2 * KEY_AGREEMENT_RANDOM_LEN];
    System.arraycopy(ctx.responderRandom,
                     0,
                     ecdh1_derive_params.pSharedData,
                     0,
                     KEY_AGREEMENT_RANDOM_LEN);
    System.arraycopy(ctx.initiatorRandom,
                     0,
                     ecdh1_derive_params.pSharedData,
                     KEY_AGREEMENT_RANDOM_LEN,
                     KEY_AGREEMENT_RANDOM_LEN);
    ecdh1_derive_params.pPublicData = ctx.responderEphPubKey;

    ckm_ecdh1_derive.mechanism = PKCS11Constants.CKM_ECDH1_DERIVE;
    ckm_ecdh1_derive.pParameter = ecdh1_derive_params;

    hMasterSecret = p11.C_DeriveKey(hSession,
                                    ckm_ecdh1_derive,
                                    ctx.hInitiatorEphPrivKey,
                                    masterSecretTemplate,
                                    true);
    /* Validate POI MAC */

    extractParams = 0;
    ckm_extract_key_from_key.mechanism = PKCS11Constants.CKM_EXTRACT_KEY_FROM_KEY;
    ckm_extract_key_from_key.pParameter = extractParams;

    responderMACKeyTemplate[0] = new CK_ATTRIBUTE();
    responderMACKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    responderMACKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    responderMACKeyTemplate[1] = new CK_ATTRIBUTE();
    responderMACKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    responderMACKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    responderMACKeyTemplate[2] = new CK_ATTRIBUTE();
    responderMACKeyTemplate[2].type = PKCS11Constants.CKA_VALUE_LEN;
    responderMACKeyTemplate[2].pValue = macKeyValueLen;

    responderMACKeyTemplate[3] = new CK_ATTRIBUTE();
    responderMACKeyTemplate[3].type = PKCS11Constants.CKA_VERIFY;
    responderMACKeyTemplate[3].pValue = true;

    hResponderMACKey = p11.C_DeriveKey(hSession,
                                       ckm_extract_key_from_key,
                                       hMasterSecret,
                                       responderMACKeyTemplate,
                                       true);

    ckm_aes_cmac.mechanism = PKCS11Constants.CKM_AES_CMAC;

    p11.C_VerifyInit(hSession, ckm_aes_cmac, hResponderMACKey, true);
    p11.C_VerifyUpdate(hSession, ctx.initiatorRandom);
    p11.C_VerifyUpdate(hSession, ctx.initiatorAuthPubKey);
    p11.C_VerifyUpdate(hSession, ctx.responderRandom);
    p11.C_VerifyUpdate(hSession, ctx.responderEphPubKey);
    p11.C_VerifyUpdate(hSession, ctx.initiatorEphPubKey);
    p11.C_VerifyUpdate(hSession, ctx.initiatorSignature);
    p11.C_VerifyFinal(hSession, ctx.responderCMAC);
    p11.C_DestroyObject(hSession, hResponderMACKey);

    /* Generate HSM MAC */

    extractParams = 256;
    ckm_extract_key_from_key.mechanism = PKCS11Constants.CKM_EXTRACT_KEY_FROM_KEY;
    ckm_extract_key_from_key.pParameter = extractParams;

    initiatorMACKeyTemplate[0] = new CK_ATTRIBUTE();
    initiatorMACKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    initiatorMACKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    initiatorMACKeyTemplate[1] = new CK_ATTRIBUTE();
    initiatorMACKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    initiatorMACKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    initiatorMACKeyTemplate[2] = new CK_ATTRIBUTE();
    initiatorMACKeyTemplate[2].type = PKCS11Constants.CKA_VALUE_LEN;
    initiatorMACKeyTemplate[2].pValue = macKeyValueLen;

    initiatorMACKeyTemplate[3] = new CK_ATTRIBUTE();
    initiatorMACKeyTemplate[3].type = PKCS11Constants.CKA_SIGN;
    initiatorMACKeyTemplate[3].pValue = true;

    hInitiatorMACKey = p11.C_DeriveKey(hSession,
                                       ckm_extract_key_from_key,
                                       hMasterSecret,
                                       initiatorMACKeyTemplate,
                                       true);

    p11.C_SignInit(hSession, ckm_aes_cmac, hInitiatorMACKey, true);
    p11.C_SignUpdate(hSession, ctx.initiatorRandom);
    p11.C_SignUpdate(hSession, ctx.initiatorAuthPubKey);
    p11.C_SignUpdate(hSession, ctx.responderRandom);
    p11.C_SignUpdate(hSession, ctx.responderEphPubKey);
    p11.C_SignUpdate(hSession, ctx.initiatorEphPubKey);
    p11.C_SignUpdate(hSession, ctx.initiatorSignature);
    ctx.initiatorCMAC = p11.C_SignFinal(hSession);

    /* Derive Reincarnation Key */

    p11.C_DestroyObject(hSession, hInitiatorMACKey);

    hReincarnationKey = deriveReincarnationKey(ctx.incKeyDerivationInfo);
    ephKBPKTemplate[0] = new CK_ATTRIBUTE();
    ephKBPKTemplate[0].type = PKCS11Constants.CKA_CLASS;
    ephKBPKTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    ephKBPKTemplate[1] = new CK_ATTRIBUTE();
    ephKBPKTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    ephKBPKTemplate[1].pValue = PKCS11Constants.CKK_AES;

    ephKBPKTemplate[2] = new CK_ATTRIBUTE();
    ephKBPKTemplate[2].type = PKCS11Constants.CKA_VALUE_LEN;
    ephKBPKTemplate[2].pValue = macKeyValueLen;

    ephKBPKTemplate[3] = new CK_ATTRIBUTE();
    ephKBPKTemplate[3].type = PKCS11Constants.CKA_WRAP;
    ephKBPKTemplate[3].pValue = true;

    ephKBPKTemplate[4] = new CK_ATTRIBUTE();
    ephKBPKTemplate[4].type = CKA_X9_143_KBH;
    ephKBPKTemplate[4].pValue = new String("D0016K1AE00N0020").toCharArray();

    extractParams = 512;
    ckm_extract_key_from_key.mechanism = PKCS11Constants.CKM_EXTRACT_KEY_FROM_KEY;
    ckm_extract_key_from_key.pParameter = extractParams;

    hEphKBPKey = p11.C_DeriveKey(hSession,
                                 ckm_extract_key_from_key,
                                 hMasterSecret,
                                 ephKBPKTemplate,
                                 true);

    ckm_x9_143_key_wrap.mechanism = CKM_X9_143_KEY_WRAP;
    ckm_x9_143_key_wrap.pParameter = new String("D0016K1AD00N0000").toCharArray();

    ctx.initiatorKeyblock = p11.C_WrapKey(hSession,
                                          ckm_x9_143_key_wrap,
                                          hEphKBPKey,
                                          hReincarnationKey,
                                          true);
    p11.C_DestroyObject(hSession, hEphKBPKey);
    p11.C_DestroyObject(hSession, hReincarnationKey);
    p11.C_DestroyObject(hSession, hMasterSecret);
  }

  public byte[] getAppKeyKeyblock(byte[] reincarnationKeyDerivationInput)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] appKeySearchTemplate = new CK_ATTRIBUTE[3];
    CK_MECHANISM ckm_x9_143_key_wrap = new CK_MECHANISM();
    long hReincarnationKey;
    long hApplicationSignKey;
    long[] hKeys;
    byte[] keyBlock;

    appKeySearchTemplate[0] = new CK_ATTRIBUTE();
    appKeySearchTemplate[0].type = PKCS11Constants.CKA_CLASS;
    appKeySearchTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    appKeySearchTemplate[1] = new CK_ATTRIBUTE();
    appKeySearchTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    appKeySearchTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    appKeySearchTemplate[2] = new CK_ATTRIBUTE();
    appKeySearchTemplate[2].type = PKCS11Constants.CKA_LABEL;
    appKeySearchTemplate[2].pValue = new String("APPLICATION_SIGN").toCharArray();

    p11.C_FindObjectsInit(hSession, appKeySearchTemplate, true);
    hKeys = p11.C_FindObjects(hSession, 2);
    p11.C_FindObjectsFinal(hSession);

    if (hKeys.length != 1) {
      throw new IOException("APPLICATION_SIGN key not found.");
    }

    hApplicationSignKey = hKeys[0];
    hReincarnationKey = deriveReincarnationKey(reincarnationKeyDerivationInput);

    ckm_x9_143_key_wrap.mechanism = CKM_X9_143_KEY_WRAP;
    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RV00N0000").toCharArray();

    keyBlock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hReincarnationKey,
                             hApplicationSignKey,
                             true);

    p11.C_DestroyObject(hSession, hReincarnationKey);

    return keyBlock;
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

  protected long deriveReincarnationKey(byte[] derivationInfo)
      throws IOException, PKCS11Exception
  {
    CK_MECHANISM ckm_sha256 = new CK_MECHANISM();
    CK_MECHANISM ckm_aes_ecb_encrypt_data = new CK_MECHANISM();
    CK_ATTRIBUTE[] reincarnationKeyTemplate = new CK_ATTRIBUTE[6];
    CK_KEY_DERIVATION_STRING_DATA derivationData = new CK_KEY_DERIVATION_STRING_DATA();
    long hReincarnationMasterKey = getReincarnationMasterKey();

    ckm_sha256.mechanism = PKCS11Constants.CKM_SHA256;

    p11.C_DigestInit(hSession, ckm_sha256, true);
    p11.C_DigestUpdate(hSession, derivationInfo);
    derivationData.pData = p11.C_DigestFinal(hSession);

    reincarnationKeyTemplate[0] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    reincarnationKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    reincarnationKeyTemplate[1] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    reincarnationKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    reincarnationKeyTemplate[2] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[2].type = PKCS11Constants.CKA_WRAP;
    reincarnationKeyTemplate[2].pValue = true;

    reincarnationKeyTemplate[3] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[3].type = PKCS11Constants.CKA_UNWRAP;
    reincarnationKeyTemplate[3].pValue = true;

    reincarnationKeyTemplate[4] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[4].type = PKCS11Constants.CKA_LABEL;
    reincarnationKeyTemplate[4].pValue = new String("X9_143_MASTER_KBPK").toCharArray();

    reincarnationKeyTemplate[5] = new CK_ATTRIBUTE();
    reincarnationKeyTemplate[5].type = CKA_X9_143_KBH;
    reincarnationKeyTemplate[5].pValue = new String("D0016K1AD00N0000").toCharArray();

    ckm_aes_ecb_encrypt_data.mechanism  = PKCS11Constants.CKM_AES_ECB_ENCRYPT_DATA;
    ckm_aes_ecb_encrypt_data.pParameter = derivationData;

    return p11.C_DeriveKey(hSession,
                           ckm_aes_ecb_encrypt_data,
                           hReincarnationMasterKey,
                           reincarnationKeyTemplate,
                           true);
  }

  protected long getReincarnationMasterKey()
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] reincarnationMasterKeyTemplate = new CK_ATTRIBUTE[3];
    long[] hKeys;

    reincarnationMasterKeyTemplate[0] = new CK_ATTRIBUTE();
    reincarnationMasterKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    reincarnationMasterKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    reincarnationMasterKeyTemplate[1] = new CK_ATTRIBUTE();
    reincarnationMasterKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    reincarnationMasterKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    reincarnationMasterKeyTemplate[2] = new CK_ATTRIBUTE();
    reincarnationMasterKeyTemplate[2].type = PKCS11Constants.CKA_LABEL;
    reincarnationMasterKeyTemplate[2].pValue = new String("000DEB06").toCharArray();

    p11.C_FindObjectsInit(hSession, reincarnationMasterKeyTemplate, true);
    hKeys = p11.C_FindObjects(hSession, 1);
    p11.C_FindObjectsFinal(hSession);

    if (hKeys.length != 1) {
      throw new IOException("Did not find Reincarnation Master Key.");
    }

    return hKeys[0];
  }

  private long[] generateKeyLoadingDeviceAuthenticationKeyPair()
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] pubKeyTemplate = new CK_ATTRIBUTE[3];

    pubKeyTemplate[0] = new CK_ATTRIBUTE();
    pubKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    pubKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    pubKeyTemplate[1] = new CK_ATTRIBUTE();
    pubKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    pubKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    pubKeyTemplate[2] = new CK_ATTRIBUTE();
    pubKeyTemplate[2].type = PKCS11Constants.CKA_EC_PARAMS;
    pubKeyTemplate[2].pValue = OID_SECP256R1;

    CK_ATTRIBUTE[] privKeyTemplate = new CK_ATTRIBUTE[3];

    privKeyTemplate[0] = new CK_ATTRIBUTE();
    privKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    privKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    privKeyTemplate[1] = new CK_ATTRIBUTE();
    privKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    privKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    privKeyTemplate[2] = new CK_ATTRIBUTE();
    privKeyTemplate[2].type = PKCS11Constants.CKA_SIGN;
    privKeyTemplate[2].pValue = true;

    CK_MECHANISM ckm_ec_key_pair_gen = new CK_MECHANISM();
    ckm_ec_key_pair_gen.mechanism = PKCS11Constants.CKM_EC_KEY_PAIR_GEN;

    return p11.C_GenerateKeyPair(hSession,
                                 ckm_ec_key_pair_gen,
                                 pubKeyTemplate,
                                 privKeyTemplate,
                                 true);
  }

  private long[] createDebugKeyLoadingDeviceAuthenticationKeyPair()
      throws IOException, PKCS11Exception
  {
    byte[] privateKeyValue = HexFormat.ofDelimiter(":").parseHex(
      "6b:d8:68:98:85:d1:b1:1a:6e:2e:54:3c:08:79:34:" +
      "f1:db:d2:4e:4b:6c:43:f2:2c:85:09:e8:4c:d5:a0:" +
      "c7:a8");
    byte[] publicKeyValue = HexFormat.ofDelimiter(":").parseHex(
      "04:13:6d:ca:9f:fd:e9:16:30:53:2a:25:c0:49:70:" +
      "5f:d4:e3:d8:f1:04:51:69:b2:1c:e3:6a:70:63:8d:" +
      "bd:35:07:b5:2a:8f:7b:6b:33:20:ed:f4:fe:16:6a:" +
      "d3:ac:8e:2a:3c:79:a1:f5:bf:18:93:2f:e4:81:b7:" +
      "70:ce:6e:20:b1");

    CK_ATTRIBUTE[] pubKeyTemplate = new CK_ATTRIBUTE[5];

    pubKeyTemplate[0] = new CK_ATTRIBUTE();
    pubKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    pubKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    pubKeyTemplate[1] = new CK_ATTRIBUTE();
    pubKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    pubKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    pubKeyTemplate[2] = new CK_ATTRIBUTE();
    pubKeyTemplate[2].type = PKCS11Constants.CKA_EC_PARAMS;
    pubKeyTemplate[2].pValue = OID_SECP256R1;

    pubKeyTemplate[3] = new CK_ATTRIBUTE();
    pubKeyTemplate[3].type = PKCS11Constants.CKA_EC_POINT;
    pubKeyTemplate[3].pValue = publicKeyValue;

    pubKeyTemplate[4] = new CK_ATTRIBUTE();
    pubKeyTemplate[4].type = PKCS11Constants.CKA_VERIFY;
    pubKeyTemplate[4].pValue = true;

    long[] hKeyPair = new long[2];
    hKeyPair[0] = p11.C_CreateObject(hSession, pubKeyTemplate, true);

    CK_ATTRIBUTE[] privKeyTemplate = new CK_ATTRIBUTE[5];

    privKeyTemplate[0] = new CK_ATTRIBUTE();
    privKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    privKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    privKeyTemplate[1] = new CK_ATTRIBUTE();
    privKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    privKeyTemplate[1].pValue = PKCS11Constants.CKK_EC;

    privKeyTemplate[2] = new CK_ATTRIBUTE();
    privKeyTemplate[2].type = PKCS11Constants.CKA_EC_PARAMS;
    privKeyTemplate[2].pValue = OID_SECP256R1;

    privKeyTemplate[3] = new CK_ATTRIBUTE();
    privKeyTemplate[3].type = PKCS11Constants.CKA_VALUE;
    privKeyTemplate[3].pValue = privateKeyValue;

    privKeyTemplate[4] = new CK_ATTRIBUTE();
    privKeyTemplate[4].type = PKCS11Constants.CKA_SIGN;
    privKeyTemplate[4].pValue = true;

    hKeyPair[1] = p11.C_CreateObject(hSession, privKeyTemplate, true);

    return hKeyPair;
  }

  private long generateManufacturingResetSecretMasterKey()
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] mfgResetMasterKeyTemplate = new CK_ATTRIBUTE[5];

    mfgResetMasterKeyTemplate[0] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    mfgResetMasterKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    mfgResetMasterKeyTemplate[1] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    mfgResetMasterKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    mfgResetMasterKeyTemplate[2] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[2].type = PKCS11Constants.CKA_VALUE_LEN;
    mfgResetMasterKeyTemplate[2].pValue = 32;

    mfgResetMasterKeyTemplate[3] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[3].type = PKCS11Constants.CKA_LABEL;
    mfgResetMasterKeyTemplate[3].pValue = new String("MFG_RESET_MASTER").toCharArray();

    mfgResetMasterKeyTemplate[4] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[4].type = PKCS11Constants.CKA_ENCRYPT;
    mfgResetMasterKeyTemplate[4].pValue = true;

    CK_MECHANISM ckm_aes_key_gen = new CK_MECHANISM();
    ckm_aes_key_gen.mechanism = PKCS11Constants.CKM_AES_KEY_GEN;
    ckm_aes_key_gen.pParameter = null;

    return p11.C_GenerateKey(hSession,
                             ckm_aes_key_gen,
                             mfgResetMasterKeyTemplate,
                             true);
  }

  private long createDebugManufacturingResetSecretMasterKey()
      throws IOException, PKCS11Exception
  {
    byte[] keyValue = HexFormat.ofDelimiter(":").parseHex(
      "1f:1e:1d:1c:1b:1a:19:18:17:16:15:14:13:12:11:10:" +
      "0f:0e:0d:0c:0b:0a:09:08:07:06:05:04:03:02:01:00");

    CK_ATTRIBUTE[] mfgResetMasterKeyTemplate = new CK_ATTRIBUTE[5];

    mfgResetMasterKeyTemplate[0] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    mfgResetMasterKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    mfgResetMasterKeyTemplate[1] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    mfgResetMasterKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    mfgResetMasterKeyTemplate[2] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[2].type = PKCS11Constants.CKA_VALUE;
    mfgResetMasterKeyTemplate[2].pValue = keyValue;

    mfgResetMasterKeyTemplate[3] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[3].type = PKCS11Constants.CKA_LABEL;
    mfgResetMasterKeyTemplate[3].pValue = new String("MFG_RESET_MASTER").toCharArray();

    mfgResetMasterKeyTemplate[4] = new CK_ATTRIBUTE();
    mfgResetMasterKeyTemplate[4].type = PKCS11Constants.CKA_ENCRYPT;
    mfgResetMasterKeyTemplate[4].pValue = true;

    return p11.C_CreateObject(hSession, mfgResetMasterKeyTemplate, true);
  }

  private long[] createDebugSigningKeyPair()
      throws IOException, PKCS11Exception
  {
    byte[] modulus = HexFormat.ofDelimiter(":").parseHex(
      "00:9c:ba:c2:5a:bf:cc:c5:4f:20:0c:4f:6f:6c:51:" +
      "4f:5c:0a:ab:80:b8:6b:10:c4:9b:2b:c4:52:32:09:" +
      "4b:3b:27:94:6a:1d:d5:4c:a8:5c:a0:c0:76:95:7b:" +
      "26:04:b1:13:7e:78:27:d6:0c:b4:e8:b0:2d:92:52:" +
      "8a:fb:69:ff:42:10:aa:56:0c:83:c8:65:6e:ba:0d:" +
      "5f:8e:f7:2b:29:92:fc:42:2d:2d:f9:80:f5:85:21:" +
      "87:ea:ac:40:a8:cb:d0:a8:3b:e2:d2:ec:f0:14:48:" +
      "0e:cf:2b:8a:4b:a4:cd:a1:05:5b:17:66:1d:de:6e:" +
      "44:fe:46:a3:0d:d0:69:bf:8c:ad:a9:16:68:51:eb:" +
      "79:91:20:e6:81:03:07:89:40:55:4b:eb:cf:67:f8:" +
      "31:c7:1c:54:4e:52:0b:60:e8:a2:50:07:d1:cf:ce:" +
      "12:26:cd:8e:82:8d:4e:64:a9:f7:c7:21:99:25:07:" +
      "dd:c5:d5:5f:f4:63:fa:cc:2b:da:06:5c:59:67:b0:" +
      "06:35:e9:aa:92:45:35:e5:a0:03:ff:1c:02:b5:c7:" +
      "4e:94:4b:6e:ad:73:9d:ce:6f:09:b3:b1:8f:60:6c:" +
      "a2:fa:cd:77:0f:cc:27:e6:36:58:b3:52:f7:8f:be:" +
      "49:98:b7:e9:60:fd:97:57:cd:ea:d3:0b:df:a2:42:" +
      "f7:44:d3:87:de:e0:10:03:94:da:fc:bc:dd:be:93:" +
      "b3:4a:2b:58:dc:96:12:f2:6f:23:ba:3b:37:fe:fc:" +
      "18:1f:75:7d:54:01:0e:be:3d:18:13:b3:28:b9:34:" +
      "2c:d5:fb:c5:33:bd:87:bd:3b:e4:1d:d7:02:3d:1c:" +
      "72:65:72:43:43:36:a8:fa:e6:73:2d:a4:61:e8:02:" +
      "9c:3a:56:4d:1c:d1:76:9c:8c:aa:5f:1b:eb:1c:4a:" +
      "f5:b9:b8:6f:41:4b:27:87:de:f6:94:1f:dd:e6:f1:" +
      "a9:c2:02:c2:4f:a3:fc:a4:03:5a:d9:6f:78:fd:84:" +
      "f0:e5:fd:3d:a5:4d:1b:ad:5b:4b");
    byte[] publicExponent = HexFormat.ofDelimiter(":").parseHex("01:00:01");
    byte[] privateExponent = HexFormat.ofDelimiter(":").parseHex(
      "1b:bf:3a:9a:5b:5b:72:d1:0b:d0:f2:1d:3d:55:75:" +
      "d1:cb:37:ca:ad:9b:92:d7:e3:ca:cf:52:67:f6:5a:" +
      "41:ca:43:8d:b7:e3:63:d3:68:b6:b2:ec:2b:91:a6:" +
      "4c:ed:56:90:ac:d0:0c:a6:aa:3e:89:b6:3f:b1:ea:" +
      "4c:ed:56:90:ac:d0:0c:a6:aa:3e:89:b6:3f:b1:ea:" +
      "56:11:51:76:0e:42:41:c2:ac:70:05:79:21:6a:2d:" +
      "b1:3f:53:fe:63:bd:d2:2f:72:74:ba:1e:7d:67:ce:" +
      "ca:be:9d:21:e5:17:77:39:46:f2:65:ad:29:42:0a:" +
      "91:25:d9:b1:83:a7:3d:eb:17:51:e0:ab:c8:0d:c8:" +
      "9c:25:c0:47:39:4f:07:70:ed:eb:c2:02:4d:02:7d:" +
      "be:b2:4e:ec:a7:2f:25:de:e4:5a:f1:10:02:8d:9f:" +
      "ae:6a:f7:07:f8:a1:78:12:27:6e:38:bc:2d:e8:83:" +
      "31:28:72:22:ed:c6:63:ae:ed:a5:6c:ee:d2:6d:fe:" +
      "96:c0:2c:61:35:74:18:c2:68:b2:78:95:ba:56:99:" +
      "de:16:10:41:ac:c7:52:4f:b3:0f:33:12:0b:bc:eb:" +
      "c8:80:ab:a6:31:1b:83:35:94:1d:92:3d:10:68:b5:" +
      "07:cf:74:9f:05:2d:76:19:42:a6:4a:fc:ce:85:83:" +
      "6f:40:63:6b:0a:5f:b6:21:b3:ed:f4:66:ee:ad:93:" +
      "42:46:00:58:6c:cd:2a:6d:c3:66:52:12:e0:bb:11:" +
      "00:2b:c7:9c:f6:75:ef:05:12:cd:ef:6d:a8:fd:78:" +
      "3d:be:7d:dd:64:77:9c:e4:e2:56:3a:c9:5a:6f:b8:" +
      "67:98:60:57:74:1f:a0:4a:96:e6:cc:0e:01:89:78:" +
      "e9:09:8a:66:c0:97:01:f1:a4:a3:32:62:1e:76:1c:" +
      "c9:a5:0b:90:60:55:7a:09:f5:2d:0e:6b:71:bf:09:" +
      "50:ca:e2:6d:07:5d:ac:76:71:09:51:eb:6e:0c:21:" +
      "db:39:bf:bf:d0:98:7d:3b:69:58:29:ca:1e:af:b0:" +
      "17:79:b0:8e:5e:b0:ee:6e:09");
    byte[] prime1 = HexFormat.ofDelimiter(":").parseHex(
      "00:ba:25:55:b8:2c:f7:5f:43:c7:4c:63:70:f9:d6:" +
      "e4:c4:b1:5b:77:2d:7e:52:3d:d9:91:35:04:e1:ff:" +
      "46:d8:39:81:f0:70:47:f6:93:a1:43:37:6b:f6:60:" +
      "20:91:d0:62:15:6d:cb:64:f3:51:5b:6d:86:d9:8b:" +
      "35:34:4a:bb:61:52:a9:63:7c:ff:f1:89:c2:68:f8:" +
      "6d:c3:41:b2:13:20:9e:21:c4:7e:4b:ad:94:18:8f:" +
      "f5:ec:61:76:f0:99:5f:86:d8:a5:d4:e4:24:0c:af:" +
      "3a:65:06:8c:43:3c:40:11:6c:a1:de:b4:bb:4c:fb:" +
      "05:dd:04:21:7b:3b:3c:85:11:3f:47:cd:01:45:c2:" +
      "81:21:cc:3e:37:48:08:b9:63:87:01:e2:9e:bd:f5:" +
      "54:42:14:db:fc:5e:87:0f:03:04:82:70:91:1f:eb:" +
      "0a:c3:ea:c4:19:a5:23:03:d6:56:3c:c2:45:39:d8:" +
      "53:14:2e:c4:20:18:7f:ea:af:80:dc:a5:29");
    byte[] prime2 = HexFormat.ofDelimiter(":").parseHex(
      "00:d7:8b:75:06:23:bb:77:d2:b6:0d:31:d2:23:32:" +
      "7d:46:3e:48:56:a3:ad:81:38:93:42:ba:ca:17:88:" +
      "69:db:54:e9:23:f2:a0:5a:c9:fe:e4:3e:ed:74:38:" +
      "d0:73:1d:c6:0b:ef:e4:90:61:e5:55:5d:06:3e:0c:" +
      "7d:37:de:38:bc:a1:ad:09:a7:5c:4c:18:89:71:e0:" +
      "96:44:76:e4:18:96:4d:24:7b:88:54:36:b7:01:41:" +
      "cc:d3:44:0e:3b:0e:63:26:da:42:1d:5a:56:93:1e:" +
      "d0:7b:68:22:4d:c7:bd:88:af:05:a7:95:54:d8:f5:" +
      "35:a6:eb:ec:8e:3d:ca:55:e4:97:f2:08:c3:3a:1a:" +
      "aa:11:c9:c8:a4:80:d4:b9:21:0b:a0:e6:19:76:a4:" +
      "55:4b:c3:e5:13:7f:1a:3a:9a:c6:1a:00:e9:e9:20:" +
      "25:23:a1:e0:d4:29:2d:19:f3:e1:6d:cc:68:40:91:" +
      "64:e4:f0:65:c8:88:f6:df:bb:b4:68:37:53");
    byte[] exponent1 = HexFormat.ofDelimiter(":").parseHex(
      "1c:6a:1e:8f:a2:ad:90:29:34:7f:00:9e:fa:44:47:" +
      "5f:8f:03:3b:4a:02:82:63:56:96:c7:d5:1a:fa:70:" +
      "c2:08:e5:40:e2:a2:d9:8c:e7:8c:ef:24:d7:d3:0c:" +
      "a5:b5:7d:83:6f:e3:20:61:d4:05:74:ce:a4:de:8f:" +
      "1c:90:5b:d2:1b:60:28:03:a8:be:22:1d:3f:10:cd:" +
      "10:85:32:bd:a0:b9:02:c6:8f:d0:d5:8a:49:c5:8f:" +
      "f0:a9:60:12:92:16:f6:ea:93:45:d0:1d:80:a2:2e:" +
      "f3:c4:f0:1c:43:52:34:56:b4:ce:de:1f:c9:3c:78:" +
      "44:a8:a0:c3:c2:e3:16:1c:ef:df:f1:f7:43:c2:d1:" +
      "1b:6b:a6:03:a2:47:52:a5:52:b2:82:f9:60:5d:d6:" +
      "a2:cf:b4:54:ed:c9:08:0d:99:de:9f:78:47:bb:05:" +
      "45:aa:cc:8c:6d:9c:ce:b5:25:2c:d9:2a:e0:0d:99:" +
      "80:f7:32:91:03:ac:63:12:af:44:ff:21");
    byte[] exponent2 = HexFormat.ofDelimiter(":").parseHex(
      "27:9b:b5:de:71:f4:82:19:53:60:eb:55:cd:27:fb:" +
      "03:4c:70:a8:93:1d:50:10:a0:66:f3:c2:2d:3e:e1:" +
      "0c:ef:f6:83:a7:93:35:fb:c6:7e:14:de:37:ac:35:" +
      "09:e2:5f:c1:53:cc:f1:87:3b:c8:4a:f6:d9:b2:1b:" +
      "d4:87:5e:6f:b3:5a:03:db:20:47:cf:7e:7a:51:a5:" +
      "eb:60:d9:9b:77:0b:27:f8:17:5e:3a:4b:b8:cc:69:" +
      "a2:2c:f0:5c:83:d1:4e:93:6e:f0:cb:e7:fa:d2:ff:" +
      "c6:9d:a5:28:1b:db:45:61:bc:2d:46:70:b8:09:9f:" +
      "59:54:23:3d:24:37:a9:3e:ff:78:f7:89:40:85:23:" +
      "d9:6e:f6:12:dc:a5:ba:7b:4b:12:bf:15:d0:ca:73:" +
      "ee:1f:4a:5a:21:d2:25:10:c9:f4:7f:54:7c:85:6e:" +
      "ad:b4:92:87:0a:26:02:8d:e1:4f:f6:ee:f5:66:82:" +
      "5e:db:9a:28:96:9a:9f:14:74:1b:6e:87");
    byte[] coefficient = HexFormat.ofDelimiter(":").parseHex(
      "2e:05:1e:30:dd:09:a7:6e:c7:26:dd:be:df:cb:2f:" +
      "27:dc:05:da:d6:02:1f:62:38:cf:a5:1d:f9:ab:2a:" +
      "75:2b:db:61:88:bc:a3:eb:a6:19:22:5d:00:08:2f:" +
      "dd:b7:4f:25:98:8f:7f:96:0b:91:a0:d2:10:01:f8:" +
      "f3:67:c0:c1:c9:96:b4:71:a8:92:88:52:0c:96:03:" +
      "59:5d:d8:7b:37:70:54:34:d6:c8:42:4f:7c:59:df:" +
      "75:80:19:56:0c:08:85:12:1a:c9:28:53:e9:46:49:" +
      "af:17:24:80:4c:f6:1c:ab:49:5c:ae:32:5b:dc:85:" +
      "b6:18:32:b8:ba:8f:e3:6f:83:5b:ec:27:e8:83:de:" +
      "56:e9:3e:fa:5d:c4:8a:1d:2e:4e:f2:4d:5a:23:89:" +
      "05:3d:1b:2b:d7:01:99:c3:ae:0e:90:0f:15:5d:56:" +
      "82:68:40:50:e1:ba:d9:d5:82:81:de:6c:78:fe:d0:" +
      "ea:c2:7d:08:e9:ba:a4:19:15:f7:44:98");

    CK_ATTRIBUTE[] signingPublicKeyTemplate = new CK_ATTRIBUTE[8];

    signingPublicKeyTemplate[0] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    signingPublicKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    signingPublicKeyTemplate[1] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    signingPublicKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    signingPublicKeyTemplate[2] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[2].type = PKCS11Constants.CKA_MODULUS;
    signingPublicKeyTemplate[2].pValue = modulus;

    signingPublicKeyTemplate[3] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[3].type = PKCS11Constants.CKA_MODULUS_BITS;
    signingPublicKeyTemplate[3].pValue = 3072;

    signingPublicKeyTemplate[4] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[4].type = PKCS11Constants.CKA_PUBLIC_EXPONENT;
    signingPublicKeyTemplate[4].pValue = publicExponent;

    signingPublicKeyTemplate[5] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[5].type = PKCS11Constants.CKA_TOKEN;
    signingPublicKeyTemplate[5].pValue = false;

    signingPublicKeyTemplate[6] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[6].type = PKCS11Constants.CKA_VERIFY;
    signingPublicKeyTemplate[6].pValue = true;

    signingPublicKeyTemplate[7] = new CK_ATTRIBUTE();
    signingPublicKeyTemplate[7].type = PKCS11Constants.CKA_EXTRACTABLE;
    signingPublicKeyTemplate[7].pValue = true;

    long[] hKeyPair = new long[2];

    hKeyPair[0] = p11.C_CreateObject(hSession,
                                     signingPublicKeyTemplate,
                                     true);

    CK_ATTRIBUTE[] signingPrivateKeyTemplate = new CK_ATTRIBUTE[13];

    signingPrivateKeyTemplate[0] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    signingPrivateKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    signingPrivateKeyTemplate[1] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    signingPrivateKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    signingPrivateKeyTemplate[2] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[2].type = PKCS11Constants.CKA_MODULUS;
    signingPrivateKeyTemplate[2].pValue = modulus;

    signingPrivateKeyTemplate[3] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[3].type = PKCS11Constants.CKA_PUBLIC_EXPONENT;
    signingPrivateKeyTemplate[3].pValue = publicExponent;

    signingPrivateKeyTemplate[4] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[4].type = PKCS11Constants.CKA_PRIVATE_EXPONENT;
    signingPrivateKeyTemplate[4].pValue = privateExponent;

    signingPrivateKeyTemplate[5] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[5].type = PKCS11Constants.CKA_PRIME_1;
    signingPrivateKeyTemplate[5].pValue = prime1;

    signingPrivateKeyTemplate[6] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[6].type = PKCS11Constants.CKA_PRIME_2;
    signingPrivateKeyTemplate[6].pValue = prime2;

    signingPrivateKeyTemplate[7] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[7].type = PKCS11Constants.CKA_EXPONENT_1;
    signingPrivateKeyTemplate[7].pValue = exponent1;

    signingPrivateKeyTemplate[8] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[8].type = PKCS11Constants.CKA_EXPONENT_2;
    signingPrivateKeyTemplate[8].pValue = exponent2;

    signingPrivateKeyTemplate[9] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[9].type = PKCS11Constants.CKA_COEFFICIENT;
    signingPrivateKeyTemplate[9].pValue = coefficient;

    signingPrivateKeyTemplate[10] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[10].type = PKCS11Constants.CKA_TOKEN;
    signingPrivateKeyTemplate[10].pValue = false;

    signingPrivateKeyTemplate[11] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[11].type = PKCS11Constants.CKA_SIGN;
    signingPrivateKeyTemplate[11].pValue = true;

    signingPrivateKeyTemplate[12] = new CK_ATTRIBUTE();
    signingPrivateKeyTemplate[12].type = PKCS11Constants.CKA_EXTRACTABLE;
    signingPrivateKeyTemplate[12].pValue = true;

    hKeyPair[1] = p11.C_CreateObject(hSession,
                                     signingPrivateKeyTemplate,
                                     true);

    return hKeyPair;
  }

  private long[] generateSigningKeyPair()
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] applicationSigningPublicKeyTemplate = new CK_ATTRIBUTE[7];

    applicationSigningPublicKeyTemplate[0] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    applicationSigningPublicKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    applicationSigningPublicKeyTemplate[1] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    applicationSigningPublicKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    applicationSigningPublicKeyTemplate[2] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[2].type = PKCS11Constants.CKA_MODULUS_BITS;
    applicationSigningPublicKeyTemplate[2].pValue = 3072;

    applicationSigningPublicKeyTemplate[3] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[3].type = PKCS11Constants.CKA_PUBLIC_EXPONENT;
    applicationSigningPublicKeyTemplate[3].pValue = HexFormat.of().parseHex("010001");

    applicationSigningPublicKeyTemplate[4] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[4].type = PKCS11Constants.CKA_TOKEN;
    applicationSigningPublicKeyTemplate[4].pValue = false;

    applicationSigningPublicKeyTemplate[5] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[5].type = PKCS11Constants.CKA_VERIFY;
    applicationSigningPublicKeyTemplate[5].pValue = true;

    applicationSigningPublicKeyTemplate[6] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[6].type = PKCS11Constants.CKA_EXTRACTABLE;
    applicationSigningPublicKeyTemplate[6].pValue = true;

    CK_ATTRIBUTE[] applicationSigningPrivateKeyTemplate = new CK_ATTRIBUTE[5];

    applicationSigningPrivateKeyTemplate[0] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    applicationSigningPrivateKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    applicationSigningPrivateKeyTemplate[1] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    applicationSigningPrivateKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    applicationSigningPrivateKeyTemplate[2] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[2].type = PKCS11Constants.CKA_TOKEN;
    applicationSigningPrivateKeyTemplate[2].pValue = false;

    applicationSigningPrivateKeyTemplate[3] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[3].type = PKCS11Constants.CKA_SIGN;
    applicationSigningPrivateKeyTemplate[3].pValue = true;

    applicationSigningPrivateKeyTemplate[4] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[4].type = PKCS11Constants.CKA_EXTRACTABLE;
    applicationSigningPrivateKeyTemplate[4].pValue = true;

    CK_MECHANISM ckm_rsa_key_pair_gen = new CK_MECHANISM();
    ckm_rsa_key_pair_gen.mechanism = PKCS11Constants.CKM_RSA_PKCS_KEY_PAIR_GEN;
    ckm_rsa_key_pair_gen.pParameter = null;

    return p11.C_GenerateKeyPair(hSession,
                                 ckm_rsa_key_pair_gen,
                                 applicationSigningPublicKeyTemplate,
                                 applicationSigningPrivateKeyTemplate,
                                 true);
  }

  private long createKeyBlockProtectionKey(byte[] keyBlockProtectionKeyValue)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] keyBlockProtectionKeyTemplate = new CK_ATTRIBUTE[7];

    keyBlockProtectionKeyTemplate[0] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    keyBlockProtectionKeyTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    keyBlockProtectionKeyTemplate[1] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    keyBlockProtectionKeyTemplate[1].pValue = PKCS11Constants.CKK_AES;

    keyBlockProtectionKeyTemplate[2] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[2].type = PKCS11Constants.CKA_VALUE;
    keyBlockProtectionKeyTemplate[2].pValue = keyBlockProtectionKeyValue;

    keyBlockProtectionKeyTemplate[3] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[3].type = PKCS11Constants.CKA_WRAP;
    keyBlockProtectionKeyTemplate[3].pValue = true;

    keyBlockProtectionKeyTemplate[4] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[4].type = PKCS11Constants.CKA_UNWRAP;
    keyBlockProtectionKeyTemplate[4].pValue = true;

    keyBlockProtectionKeyTemplate[5] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[5].type = CKA_X9_143_KBH;
    keyBlockProtectionKeyTemplate[5].pValue = new String("D0016K1AB00N0000").toCharArray();

    keyBlockProtectionKeyTemplate[6] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[6].type = PKCS11Constants.CKA_TOKEN;
    keyBlockProtectionKeyTemplate[6].pValue = false;

    return p11.C_CreateObject(hSession, keyBlockProtectionKeyTemplate, true);
  }

  public RootHSMKeys generateRootHSMKeyblocks(byte[] rootHsmKbpkValue)
      throws IOException, PKCS11Exception
  {
    RootHSMKeys result = new RootHSMKeys();

    /* Add the KCV of the X9.143 Key Block Protection Key used to wrap other keys */

    result.incarnationKeyKcv =
        HexFormat.of().withUpperCase().formatHex(computeAESKeyCheckValue(rootHsmKbpkValue));

    long hRootHsmKeyblockProtectionKey = createKeyBlockProtectionKey(rootHsmKbpkValue);

    /* Generate Firmware Signing Key Pair.  Export the public key in plaintext and the private
     * key as an X9.143 Key Block.
     */

    long[] hSigningKeyPair = generateSigningKeyPair();

    CK_ATTRIBUTE[] publicKeyInfoAttr = new CK_ATTRIBUTE[1];
    publicKeyInfoAttr[0] = new CK_ATTRIBUTE();
    publicKeyInfoAttr[0].type = PKCS11Constants.CKA_PUBLIC_KEY_INFO;
    publicKeyInfoAttr[0].pValue = null;

    p11.C_GetAttributeValue(hSession, hSigningKeyPair[0], publicKeyInfoAttr, true);
    result.firmwareValidationPublicKey =
        HexFormat.of().withUpperCase().formatHex((byte[])publicKeyInfoAttr[0].pValue);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);

    CK_MECHANISM ckm_x9_143_key_wrap = new CK_MECHANISM();
    ckm_x9_143_key_wrap.mechanism = CKM_X9_143_KEY_WRAP;

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0010").toCharArray();
    byte[] keyblock = p11.C_WrapKey(hSession,
                                    ckm_x9_143_key_wrap,
                                    hRootHsmKeyblockProtectionKey,
                                    hSigningKeyPair[1],
                                    true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.firmwareSigningPrivateKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    /* Generate X9.143 Key Blocks for the Signing Keys of the HSM Application */

    hSigningKeyPair = generateSigningKeyPair();

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RV00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[0],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);
    result.hsmApplicationValidationPublicKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.hsmApplicationSigningPrivateKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    /* Generate a secp256r1 EC key pair for Key Loading Device Authentication.  Export the public
     * key in plaintext and the private key as an X9.143 Key Block.
     */

    hSigningKeyPair = generateKeyLoadingDeviceAuthenticationKeyPair();

    CK_ATTRIBUTE[] ecPointAttr = new CK_ATTRIBUTE[1];
    ecPointAttr[0] = new CK_ATTRIBUTE();
    ecPointAttr[0].type = PKCS11Constants.CKA_EC_POINT;
    p11.C_GetAttributeValue(hSession, hSigningKeyPair[0], ecPointAttr, true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);
    result.keyLoadingDeviceAuthenticationPublicKey =
        HexFormat.of().withUpperCase().formatHex((byte[])ecPointAttr[0].pValue);

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0ES00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.keyLoadingDeviceAuthenticationPrivateKeyblock =
        new String(keyblock, StandardCharsets.UTF_8);

    /* Generate Manufacturing Reset Master Key and export as X9.143 Key Block */

    long hMfgResetMasterKey = generateManufacturingResetSecretMasterKey();

    ckm_x9_143_key_wrap.pParameter = new String("D0016D0AE00N0020").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hMfgResetMasterKey,
                             true);
    p11.C_DestroyObject(hSession, hMfgResetMasterKey);
    result.manufacturingResetMasterKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    p11.C_DestroyObject(hSession, hRootHsmKeyblockProtectionKey);

    return result;
  }

  public RootHSMKeys createDebugRootHSMKeyblocks(byte[] rootHsmKbpkValue)
      throws IOException, PKCS11Exception
  {
    RootHSMKeys result = new RootHSMKeys();

    /* Add the KCV of the X9.143 Key Block Protection Key used to wrap other keys */

    result.incarnationKeyKcv =
        HexFormat.of().withUpperCase().formatHex(computeAESKeyCheckValue(rootHsmKbpkValue));

    long hRootHsmKeyblockProtectionKey = createKeyBlockProtectionKey(rootHsmKbpkValue);

    /* Generate Firmware Signing Key Pair.  Export the public key in plaintext and the private
     * key as an X9.143 Key Block.
     */

    long[] hSigningKeyPair = createDebugSigningKeyPair();

    CK_ATTRIBUTE[] publicKeyInfoAttr = new CK_ATTRIBUTE[1];
    publicKeyInfoAttr[0] = new CK_ATTRIBUTE();
    publicKeyInfoAttr[0].type = PKCS11Constants.CKA_PUBLIC_KEY_INFO;
    publicKeyInfoAttr[0].pValue = null;

    p11.C_GetAttributeValue(hSession, hSigningKeyPair[0], publicKeyInfoAttr, true);
    result.firmwareValidationPublicKey =
        HexFormat.of().withUpperCase().formatHex((byte[])publicKeyInfoAttr[0].pValue);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);

    CK_MECHANISM ckm_x9_143_key_wrap = new CK_MECHANISM();
    ckm_x9_143_key_wrap.mechanism = CKM_X9_143_KEY_WRAP;

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0010").toCharArray();
    byte[] keyblock = p11.C_WrapKey(hSession,
                                    ckm_x9_143_key_wrap,
                                    hRootHsmKeyblockProtectionKey,
                                    hSigningKeyPair[1],
                                    true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.firmwareSigningPrivateKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    /* Generate X9.143 Key Blocks for the Signing Keys of the HSM Application */

    hSigningKeyPair = createDebugSigningKeyPair();

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RV00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[0],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);
    result.hsmApplicationValidationPublicKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.hsmApplicationSigningPrivateKeyblock =
        new String(keyblock, StandardCharsets.UTF_8);

    /* Generate a secp256r1 EC key pair for Key Loading Device Authentication.  Export the public
     * key in plaintext and the private key as an X9.143 Key Block.
     */

    hSigningKeyPair = createDebugKeyLoadingDeviceAuthenticationKeyPair();

    CK_ATTRIBUTE[] ecPointAttr = new CK_ATTRIBUTE[1];
    ecPointAttr[0] = new CK_ATTRIBUTE();
    ecPointAttr[0].type = PKCS11Constants.CKA_EC_POINT;
    p11.C_GetAttributeValue(hSession, hSigningKeyPair[0], ecPointAttr, true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);
    result.keyLoadingDeviceAuthenticationPublicKey =
        HexFormat.of().withUpperCase().formatHex((byte[])ecPointAttr[0].pValue);

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0ES00N0010").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.keyLoadingDeviceAuthenticationPrivateKeyblock =
        new String(keyblock, StandardCharsets.UTF_8);

    /* Generate Manufacturing Reset Master Key and export as X9.143 Key Block */

    long hMfgResetMasterKey = createDebugManufacturingResetSecretMasterKey();

    ckm_x9_143_key_wrap.pParameter = new String("D0016D0AE00N0020").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hMfgResetMasterKey,
                             true);
    p11.C_DestroyObject(hSession, hMfgResetMasterKey);
    result.manufacturingResetMasterKeyblock = new String(keyblock, StandardCharsets.UTF_8);

    p11.C_DestroyObject(hSession, hRootHsmKeyblockProtectionKey);

    return result;
  }

  public List<Share> createRandomMasterEncryptionKeyShares()
  {
    byte[] randomMasterEncryptionKey = new byte[AES_256_KEY_LEN];

    try {
      p11.C_GenerateRandom(hSession, randomMasterEncryptionKey);
    } catch (PKCS11Exception e) {
      throw new RuntimeException("Failed to generate random master encryption key: " +
                                 e.getMessage(), e);
    }

    logger.debug("Generated random master encryption key: " +
                  HexFormat.of().formatHex(randomMasterEncryptionKey));

    return Sss4j.split(randomMasterEncryptionKey, 2, 3);
  }

  public byte[] computeAESKeyCheckValue(byte[] key)
    throws IOException, PKCS11Exception
  {
    if (key.length != AES_256_KEY_LEN) {
      throw new IllegalArgumentException("Key length must be " + AES_256_KEY_LEN + " bytes.");
    }

    CK_ATTRIBUTE[] keyCreationTemplate = new CK_ATTRIBUTE[4];

    keyCreationTemplate[0] = new CK_ATTRIBUTE();
    keyCreationTemplate[0].type = PKCS11Constants.CKA_CLASS;
    keyCreationTemplate[0].pValue = PKCS11Constants.CKO_SECRET_KEY;

    keyCreationTemplate[1] = new CK_ATTRIBUTE();
    keyCreationTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    keyCreationTemplate[1].pValue = PKCS11Constants.CKK_AES;

    keyCreationTemplate[2] = new CK_ATTRIBUTE();
    keyCreationTemplate[2].type = PKCS11Constants.CKA_VALUE;
    keyCreationTemplate[2].pValue = key;

    keyCreationTemplate[3] = new CK_ATTRIBUTE();
    keyCreationTemplate[3].type = PKCS11Constants.CKA_SIGN;
    keyCreationTemplate[3].pValue = true;

    long hKey = p11.C_CreateObject(hSession, keyCreationTemplate, true);

    CK_MECHANISM ckm_aes_cmac = new CK_MECHANISM();
    ckm_aes_cmac.mechanism = PKCS11Constants.CKM_AES_CMAC;
    ckm_aes_cmac.pParameter = null;

    byte[] zeroBlock = HexFormat.ofDelimiter(":").parseHex
                           ("00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00");
    p11.C_SignInit(hSession, ckm_aes_cmac, hKey, true);
    byte[] kcv = p11.C_Sign(hSession, zeroBlock);
    p11.C_DestroyObject(hSession, hKey);

    return Arrays.copyOf(kcv, 5);
  }

  protected PKCS11 p11;
  protected long hSession;

  protected final static byte[] SUBJECT_PUBLIC_KEY_INFO_PREFIX =
      HexFormat.of().parseHex("3059301306072A8648CE3D020106082A8648CE3D030107034200");
  protected final static byte[] OID_SECP256R1 = HexFormat.of().parseHex("06082A8648CE3D030107");
};
