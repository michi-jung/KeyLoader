package ly.secore.KeyLoader;

import ly.secore.compute.Device;
import ly.secore.compute.HardwareSecurityModule;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class ReincarnationKeyInjection {
  public static void main(String args[]) {
    try (HardwareSecurityModule keyLoader = new HardwareSecurityModule("/usr/local/lib/libcryptok.so", 0);
         Device computeDevice = new Device(args[0]))
    {
      BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
      HardwareSecurityModule.SetIncKeyContext ctx;

      computeDevice.openServiceSession(1);

      computeDevice.lock(keyLoader.deriveMfgResetSecret(
                             computeDevice.getMfgResetSecretDerivationInput()));

      computeDevice.closeServiceSession();

      System.out.println("Please power-cycle 885-R and press <enter>");
      bufferRead.readLine();

      computeDevice.openServiceSession(1);

      ctx = keyLoader.setIncKeyStep1(computeDevice.getReincarnationKeyDerivationInfo());
      computeDevice.setIncKeyStep1(ctx);
      keyLoader.setIncKeyStep2(ctx);
      computeDevice.setIncKeyStep2(ctx);
      keyLoader.setIncKeyStep3(ctx);
      computeDevice.setIncKeyStep3(ctx);

      computeDevice.closeServiceSession();
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
