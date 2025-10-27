package ly.secore.KeyLoader;

import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import ly.secore.compute.Device;

class ComputeDeviceInfo {
  public static void main(String args[]) {
    try (Device computeDevice = new Device(args[0]))
    {
      Device.ManufacturingInfo mfgInfo;
      Device.ReincarnationInfo incInfo;
      byte[] derivationInfo;

      computeDevice.openServiceSession(1);

      mfgInfo = computeDevice.getManufacturingInfo();
      incInfo = computeDevice.getReincarnationInfo();
      derivationInfo = computeDevice.getReincarnationKeyDerivationInfo();

      computeDevice.closeServiceSession();

      System.out.format("      Device Class: %s (%s)\n", mfgInfo.getDeviceClassName(), mfgInfo.getDeviceClassUUID().toString());
      System.out.format("       Device Type: %s (%d)\n", mfgInfo.getDeviceTypeName(), (int)mfgInfo.getDeviceType());
      System.out.format("               ECL: %d\n", (int)mfgInfo.getEngineeringChangeLevel());
      System.out.format("     Serial Number: %010d\n", mfgInfo.getSerialNumber());
      System.out.format("       MAC Address: %s\n", HexFormat.ofDelimiter(":").formatHex(mfgInfo.getMACAddress()));
      System.out.format("Time of Production: %s\n", mfgInfo.getTimeOfProduction().format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.FULL)));


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
