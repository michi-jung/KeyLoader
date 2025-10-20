package ly.secore.KeyLoader;

import ly.secore.compute.ComputeDevice;
import ly.secore.compute.KeyLoader;
import java.io.FileInputStream;

class ReincarnationKeyInjection {
  public static void main(String args[]) {
    try (KeyLoader keyLoader = new KeyLoader("/usr/local/lib/libcryptok.so", 0)) {
      try (ComputeDevice computeDevice = new ComputeDevice(args[0])) {
          computeDevice.openServiceSession();
          computeDevice.closeServiceSession();
      }
      catch (Exception e)
      {
        e.printStackTrace(System.out);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
