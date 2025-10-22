package ly.secore.KeyLoader;

import ly.secore.compute.ComputeDevice;

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HexFormat;

class ComputeDeviceInfo {
  public static void main(String args[]) {
    try (ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
      ComputeDevice.ManufacturingInfo mfgInfo;
      ComputeDevice.ReincarnationInfo incInfo;
      byte[] derivationInfo;

      computeDevice.openServiceSession();

      mfgInfo = computeDevice.getManufacturingInfo();
      incInfo = computeDevice.getReincarnationInfo();
      derivationInfo = computeDevice.getReincarnationKeyDerivationInfo();

      computeDevice.closeServiceSession();

      System.out.println(HexFormat.of().formatHex(mfgInfo.getPointer().getByteArray(0, mfgInfo.size())));
      System.out.println(HexFormat.of().formatHex(incInfo.getPointer().getByteArray(0, incInfo.size())));
      System.out.println(HexFormat.of().formatHex(derivationInfo));
    }
    catch (Exception e)
    {
      e.printStackTrace(System.out);
    }
  }
}
