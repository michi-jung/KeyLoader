package ly.secore.KeyLoader;

import ly.secore.compute.ComputeDevice;
import ly.secore.compute.KeyLoader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

class AppKeyInjection {
  public static void main(String args[]) {
    try (KeyLoader keyLoader = new KeyLoader("/usr/local/lib/libcryptok.so", 0);
         ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
      computeDevice.openServiceSession();

      computeDevice.setAppKey(
          keyLoader.getAppKeyKeyblock(
              computeDevice.getReincarnationKeyDerivationInfo()));

      computeDevice.closeServiceSession();
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
