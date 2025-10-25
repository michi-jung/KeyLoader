package ly.secore.KeyLoader.Database;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProductDescriptor {
    private String productKey;
    private String type;
    private String personality;

    @JsonIgnore
    private DeviceTypeDescriptor deviceType;

    @JsonIgnore
    private DevicePersonalityDescriptor devicePersonality;

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

    public DeviceTypeDescriptor getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceTypeDescriptor deviceType) {
        this.deviceType = deviceType;
    }

    public DevicePersonalityDescriptor getDevicePersonality() {
        return devicePersonality;
    }

    public void setDevicePersonality(DevicePersonalityDescriptor devicePersonality) {
        this.devicePersonality = devicePersonality;
    }

    @Override
    public String toString() {
        return "ProductDescriptor{" +
                "productKey='" + productKey + '\'' +
                ", type='" + deviceType.toString() + '\'' +
                ", personality='" + devicePersonality.toString() + '\'' +
                '}';
    }
}
