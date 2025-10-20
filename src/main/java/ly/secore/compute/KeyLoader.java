package ly.secore.compute;

import java.io.IOException;

import iaik.pkcs.pkcs11.wrapper.CK_ATTRIBUTE;
import iaik.pkcs.pkcs11.wrapper.PKCS11;
import iaik.pkcs.pkcs11.wrapper.PKCS11Connector;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;
import iaik.pkcs.pkcs11.wrapper.PKCS11Exception;

public class KeyLoader implements AutoCloseable {
  protected PKCS11 p11;
  long hSession;

  public KeyLoader(String pkcs11ModuleFilename, long slotID) throws IOException, PKCS11Exception {
    p11 = PKCS11Connector.connectToPKCS11Module(pkcs11ModuleFilename);

    p11.C_Initialize(null, true);
    hSession = p11.C_OpenSession(slotID, PKCS11Constants.CKF_SERIAL_SESSION, 0, null);
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
