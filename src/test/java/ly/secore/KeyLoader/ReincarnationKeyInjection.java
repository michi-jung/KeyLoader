package ly.secore.KeyLoader;

import ly.secore.compute.ComputeDevice;
import ly.secore.compute.KeyLoader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HexFormat;

class ReincarnationKeyInjection {
  public static void main(String args[]) {
    try (KeyLoader keyLoader = new KeyLoader("/usr/local/lib/libcryptok.so", 0);
         ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
      BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
      KeyLoader.KeyAgreementParameters params;

      computeDevice.openServiceSession();

      computeDevice.lock(keyLoader.deriveMfgResetSecret(
                             computeDevice.getMfgResetSecretDerivationInput()));

      computeDevice.closeServiceSession();

      System.out.println("Please power-cycle 885-R and press <enter>");
      bufferRead.readLine();

      computeDevice.openServiceSession();

      params = keyLoader.keyAgreementStep1();
      params = computeDevice.setIncKeyStep1(params);

      System.out.println(HexFormat.of().formatHex(params.initiatorRandom));
      System.out.println(HexFormat.of().formatHex(params.initiatorAuthPubKey));
      System.out.println(HexFormat.of().formatHex(params.responderRandom));
      System.out.println(HexFormat.of().formatHex(params.responderEphPubKey));

      computeDevice.closeServiceSession();
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
