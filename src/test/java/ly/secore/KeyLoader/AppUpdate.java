package ly.secore.KeyLoader;

import ly.secore.compute.Device;

import java.io.FileInputStream;

class AppUpdate {
  public static void main(String args[]) {
    try (Device computeDevice = new Device(args[0]))
    {
      computeDevice.openServiceSession(1);
      computeDevice.applicationUpdate(new FileInputStream(args[1]));
      computeDevice.closeServiceSession();
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
