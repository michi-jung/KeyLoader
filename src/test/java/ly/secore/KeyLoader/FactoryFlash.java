package ly.secore.KeyLoader;

import ly.secore.compute.ComputeDevice;

import java.io.FileInputStream;

class FactoryFlash {
  public static void main(String args[]) {
    try (ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
      computeDevice.factoryFlash(new FileInputStream(args[1]));
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
