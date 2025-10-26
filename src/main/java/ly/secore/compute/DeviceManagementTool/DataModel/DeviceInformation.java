package ly.secore.compute.DeviceManagementTool.DataModel;

import ly.secore.compute.Device;

public class DeviceInformation {
    private Device.ManufacturingInfo manufacturingInfo;
    private Device.ReincarnationInfo reincarnationInfo;
    private Device.DDM885Info ddm885Info;
    boolean isDeviceConnected;

    public void setManufacturingInfo(Device.ManufacturingInfo manufacturingInfo) {
        this.manufacturingInfo = manufacturingInfo;
    }

    public Device.ManufacturingInfo getManufacturingInfo() {
        return manufacturingInfo;
    }

    public void setReincarnationInfo(Device.ReincarnationInfo reincarnationInfo) {
        this.reincarnationInfo = reincarnationInfo;
    }

    public Device.ReincarnationInfo getReincarnationInfo() {
        return reincarnationInfo;
    }

    public void setDDM885Info(Device.DDM885Info ddm885Info) {
        this.ddm885Info = ddm885Info;
    }

    public Device.DDM885Info getDDM885Info() {
        return ddm885Info;
    }

    public void setDeviceConnected(boolean isDeviceConnected) {
        this.isDeviceConnected = isDeviceConnected;
    }

    public boolean isDeviceConnected() {
        return isDeviceConnected;
    }
}
