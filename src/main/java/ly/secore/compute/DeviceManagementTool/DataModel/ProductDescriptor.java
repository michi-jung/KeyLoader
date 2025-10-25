package ly.secore.compute.DeviceManagementTool.DataModel;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProductDescriptor {
    private String productKey;
    private String type;
    private String personality;

    @JsonIgnore
    private DeviceTypeDescriptor deviceTypeDescriptor;

    @JsonIgnore
    private DevicePersonalityDescriptor devicePersonalityDescriptor;

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }

    public String getProductKey() {
        return productKey;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getPersonality() {
        return personality;
    }

    public DeviceTypeDescriptor getDeviceTypeDescriptor() {
        return deviceTypeDescriptor;
    }

    public void setDeviceType(DeviceTypeDescriptor deviceTypeDescriptor) {
        this.deviceTypeDescriptor = deviceTypeDescriptor;
    }

    public DevicePersonalityDescriptor getDevicePersonalityDescriptor() {
        return devicePersonalityDescriptor;
    }

    public void setDevicePersonality(DevicePersonalityDescriptor devicePersonalityDescriptor) {
        this.devicePersonalityDescriptor = devicePersonalityDescriptor;
    }

    @Override
    public String toString() {
        return productKey;
    }
}
