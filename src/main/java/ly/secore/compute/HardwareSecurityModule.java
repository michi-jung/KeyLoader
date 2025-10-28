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
import java.util.ArrayList;
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

  public long[] createSigningKeyPair(String label)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] applicationSigningPublicKeyTemplate = new CK_ATTRIBUTE[8];

    applicationSigningPublicKeyTemplate[0] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    applicationSigningPublicKeyTemplate[0].pValue = PKCS11Constants.CKO_PUBLIC_KEY;

    applicationSigningPublicKeyTemplate[1] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    applicationSigningPublicKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    applicationSigningPublicKeyTemplate[2] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[2].type = PKCS11Constants.CKA_LABEL;
    applicationSigningPublicKeyTemplate[2].pValue = label.toCharArray();

    applicationSigningPublicKeyTemplate[3] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[3].type = PKCS11Constants.CKA_MODULUS_BITS;
    applicationSigningPublicKeyTemplate[3].pValue = 3072;

    applicationSigningPublicKeyTemplate[4] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[4].type = PKCS11Constants.CKA_PUBLIC_EXPONENT;
    applicationSigningPublicKeyTemplate[4].pValue = HexFormat.of().parseHex("010001");

    applicationSigningPublicKeyTemplate[5] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[5].type = PKCS11Constants.CKA_TOKEN;
    applicationSigningPublicKeyTemplate[5].pValue = false;

    applicationSigningPublicKeyTemplate[6] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[6].type = PKCS11Constants.CKA_VERIFY;
    applicationSigningPublicKeyTemplate[6].pValue = true;

    applicationSigningPublicKeyTemplate[7] = new CK_ATTRIBUTE();
    applicationSigningPublicKeyTemplate[7].type = PKCS11Constants.CKA_EXTRACTABLE;
    applicationSigningPublicKeyTemplate[7].pValue = true;

    CK_ATTRIBUTE[] applicationSigningPrivateKeyTemplate = new CK_ATTRIBUTE[6];

    applicationSigningPrivateKeyTemplate[0] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[0].type = PKCS11Constants.CKA_CLASS;
    applicationSigningPrivateKeyTemplate[0].pValue = PKCS11Constants.CKO_PRIVATE_KEY;

    applicationSigningPrivateKeyTemplate[1] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[1].type = PKCS11Constants.CKA_KEY_TYPE;
    applicationSigningPrivateKeyTemplate[1].pValue = PKCS11Constants.CKK_RSA;

    applicationSigningPrivateKeyTemplate[2] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[2].type = PKCS11Constants.CKA_LABEL;
    applicationSigningPrivateKeyTemplate[2].pValue = label.toCharArray();

    applicationSigningPrivateKeyTemplate[3] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[3].type = PKCS11Constants.CKA_TOKEN;
    applicationSigningPrivateKeyTemplate[3].pValue = false;

    applicationSigningPrivateKeyTemplate[4] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[4].type = PKCS11Constants.CKA_SIGN;
    applicationSigningPrivateKeyTemplate[4].pValue = true;

    applicationSigningPrivateKeyTemplate[5] = new CK_ATTRIBUTE();
    applicationSigningPrivateKeyTemplate[5].type = PKCS11Constants.CKA_EXTRACTABLE;
    applicationSigningPrivateKeyTemplate[5].pValue = true;

    CK_MECHANISM ckm_rsa_key_pair_gen = new CK_MECHANISM();
    ckm_rsa_key_pair_gen.mechanism = PKCS11Constants.CKM_RSA_PKCS_KEY_PAIR_GEN;
    ckm_rsa_key_pair_gen.pParameter = null;

    return p11.C_GenerateKeyPair(hSession,
                                 ckm_rsa_key_pair_gen,
                                 applicationSigningPublicKeyTemplate,
                                 applicationSigningPrivateKeyTemplate,
                                 true);
  }

  public long createKeyBlockProtectionKey(String label, byte[] keyBlockProtectionKeyValue)
      throws IOException, PKCS11Exception
  {
    CK_ATTRIBUTE[] keyBlockProtectionKeyTemplate = new CK_ATTRIBUTE[8];

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
    keyBlockProtectionKeyTemplate[3].type = PKCS11Constants.CKA_LABEL;
    keyBlockProtectionKeyTemplate[3].pValue = label.toCharArray();

    keyBlockProtectionKeyTemplate[4] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[4].type = PKCS11Constants.CKA_WRAP;
    keyBlockProtectionKeyTemplate[4].pValue = true;

    keyBlockProtectionKeyTemplate[5] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[5].type = PKCS11Constants.CKA_UNWRAP;
    keyBlockProtectionKeyTemplate[5].pValue = true;

    keyBlockProtectionKeyTemplate[6] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[6].type = CKA_X9_143_KBH;
    keyBlockProtectionKeyTemplate[6].pValue = new String("D0016K1AB00N0000").toCharArray();

    keyBlockProtectionKeyTemplate[7] = new CK_ATTRIBUTE();
    keyBlockProtectionKeyTemplate[7].type = PKCS11Constants.CKA_TOKEN;
    keyBlockProtectionKeyTemplate[7].pValue = false;

    return p11.C_CreateObject(hSession, keyBlockProtectionKeyTemplate, true);
  }

  public List<String> generateRootHSMKeyblocks(byte[] rootHsmKbpkValue)
      throws IOException, PKCS11Exception
  {
    long hRootHsmKeyblockProtectionKey = createKeyBlockProtectionKey("ROOT_HSM_KBPK",
                                                                     rootHsmKbpkValue);
    long[] hSigningKeyPair = createSigningKeyPair("FW_SIGNING");

    CK_ATTRIBUTE[] publicKeyInfoAttr = new CK_ATTRIBUTE[1];
    publicKeyInfoAttr[0] = new CK_ATTRIBUTE();
    publicKeyInfoAttr[0].type = PKCS11Constants.CKA_PUBLIC_KEY_INFO;
    publicKeyInfoAttr[0].pValue = null;

    p11.C_GetAttributeValue(hSession, hSigningKeyPair[0], publicKeyInfoAttr, true);

    if (!(publicKeyInfoAttr[0].pValue instanceof byte[])) {
      throw new IOException("Failed to get public key info for signing key pair.");
    }

    List<String> result = new ArrayList<>();
    result.add(HexFormat.of().withUpperCase().formatHex((byte[])publicKeyInfoAttr[0].pValue));

    CK_MECHANISM ckm_x9_143_key_wrap = new CK_MECHANISM();
    ckm_x9_143_key_wrap.mechanism = CKM_X9_143_KEY_WRAP;

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RV00N0000").toCharArray();
    byte[] keyblock = p11.C_WrapKey(hSession,
                                    ckm_x9_143_key_wrap,
                                    hRootHsmKeyblockProtectionKey,
                                    hSigningKeyPair[0],
                                    true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);
    result.add(new String(keyblock, StandardCharsets.UTF_8));

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0000").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.add(new String(keyblock, StandardCharsets.UTF_8));

    hSigningKeyPair = createSigningKeyPair("APP_SIGNING");

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RV00N0000").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[0],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[0]);

    result.add(new String(keyblock, StandardCharsets.UTF_8));

    ckm_x9_143_key_wrap.pParameter = new String("D0016S0RS00N0000").toCharArray();
    keyblock = p11.C_WrapKey(hSession,
                             ckm_x9_143_key_wrap,
                             hRootHsmKeyblockProtectionKey,
                             hSigningKeyPair[1],
                             true);
    p11.C_DestroyObject(hSession, hSigningKeyPair[1]);
    result.add(new String(keyblock, StandardCharsets.UTF_8));

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

  protected PKCS11 p11;
  protected long hSession;

  protected final static byte[] SUBJECT_PUBLIC_KEY_INFO_PREFIX =
      HexFormat.of().parseHex("3059301306072A8648CE3D020106082A8648CE3D030107034200");
  protected final static byte[] OID_SECP256R1 = HexFormat.of().parseHex("06082A8648CE3D030107");
};
