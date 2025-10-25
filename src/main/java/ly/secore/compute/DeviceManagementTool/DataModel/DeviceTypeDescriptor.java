package ly.secore.compute.DeviceManagementTool.DataModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeviceTypeDescriptor {
    private String type;
    private String deviceClass;
    private int deviceTypeId;
    private int ecl;
    private String firmwareVersion;
    private String description;
    private String updateTime;

    @JsonIgnore
    private FirmwareDescriptor firmwareDescriptor;

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setDeviceClass(String deviceClass) {
        this.deviceClass = deviceClass;
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceTypeId(int deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public int getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setEcl(int ecl) {
        this.ecl = ecl;
    }

    public int getEcl() {
        return ecl;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public FirmwareDescriptor getFirmwareDescriptor() {
        return firmwareDescriptor;
    }

    public void setFirmwareDescriptor(FirmwareDescriptor firmwareDescriptor) {
        this.firmwareDescriptor = firmwareDescriptor;
    }

    @Override
    public String toString() {
        return "DeviceTypeDescriptor{" +
                "type='" + type + '\'' +
                ", deviceClass='" + deviceClass + '\'' +
                ", deviceTypeId='" + String.valueOf(deviceTypeId) + '\'' +
                ", ecl='" + String.valueOf(ecl) + '\'' +
                ", firmwareDescriptor='" + firmwareDescriptor.toString() + '\'' +
                ", description='" + description + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
