package ly.secore.ComputeDeviceManagementTool.DataModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DevicePersonalityDescriptor {
    private int id;
    private String personality;
    private String description;
    private int operatingMode;
    private int variant;
    private String masterKey;
    private String firmwareVersion;
    private int applicationId;
    private String updateTime;

    @JsonIgnore
    private FirmwareDescriptor firmwareDescriptor;

    @JsonIgnore
    private ApplicationDescriptor application;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getPersonality() {
        return personality;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setOperatingMode(int operatingMode) {
        this.operatingMode = operatingMode;
    }

    public int getOperatingMode() {
        return operatingMode;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    public int getVariant() {
        return variant;
    }

    public void setMasterKey(String masterKey) {
        this.masterKey = masterKey;
    }

    public String getMasterKey() {
        return masterKey;
    }

    public void setFirmware(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setApplicationId(int applicationId) {
        this.applicationId = applicationId;
    }

    public int getApplicationId() {
        return applicationId;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setFirmwareDescriptor(FirmwareDescriptor firmwareDescriptor) {
        this.firmwareDescriptor = firmwareDescriptor;
    }

    public FirmwareDescriptor getfirmwareDescriptor() {
        return firmwareDescriptor;
    }

    public void setApplication(ApplicationDescriptor application) {
        this.application = application;
    }

    public ApplicationDescriptor getApplication() {
        return application;
    }

    @Override
    public String toString() {
        return "DevicePersonalityDescriptor{" +
                "personality='" + personality + '\'' +
                ", description='" + description + '\'' +
                ", operatingMode='" + String.valueOf(operatingMode) + '\'' +
                ", personality='" + String.valueOf(variant) + '\'' +
                ", masterKey='" + masterKey + '\'' +
                ", firmwareDescriptor='" + firmwareDescriptor.toString() + '\'' +
                ", application='" + application.toString() + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
