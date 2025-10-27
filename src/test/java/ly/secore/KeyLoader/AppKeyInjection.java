package ly.secore.KeyLoader;

import ly.secore.compute.Device;
import ly.secore.compute.KeyLoader;

class AppKeyInjection {
  public static void main(String args[]) {
    try (KeyLoader keyLoader = new KeyLoader("/usr/local/lib/libcryptok.so", 0);
         Device computeDevice = new Device(args[0]))
    {
      computeDevice.openServiceSession(1);

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
