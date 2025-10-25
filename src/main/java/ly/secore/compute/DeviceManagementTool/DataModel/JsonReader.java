package ly.secore.compute.DeviceManagementTool.DataModel;

import java.io.InputStream;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * Reader class for reading JSON files.
 * This class uses Jackson to read a JSON file and print its contents.
 */
public class JsonReader {
    private static class DatabaseContent {
        public List<ProductDescriptor> productDescriptors;
        public List<DeviceTypeDescriptor> deviceTypeDescriptors;
        public List<DevicePersonalityDescriptor> devicePersonalityDescriptors;
        public List<FirmwareDescriptor> firmwareDescriptors;
        public List<ApplicationDescriptor> applicationDescriptors;
    };

    public static List<ProductDescriptor> getProductDescriptors(InputStream jsonInputStream) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonInputStream);
            DatabaseContent databaseContent;

            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            // Convert to DatabaseContent
            databaseContent = objectMapper.treeToValue(rootNode, DatabaseContent.class);

            // Set relationships between products, device types, and device personalities
            for (ProductDescriptor product : databaseContent.productDescriptors) {
                for (DeviceTypeDescriptor deviceType : databaseContent.deviceTypeDescriptors) {
                    if (deviceType.getType().equals(product.getType())) {
                        product.setDeviceType(deviceType);
                    }
                }

                for (DevicePersonalityDescriptor devicePersonality : databaseContent.devicePersonalityDescriptors) {
                    if (devicePersonality.getPersonality().equals(product.getPersonality())) {
                        product.setDevicePersonality(devicePersonality);
                    }
                }
            }

            // Set firmware versions for device types
            for (DeviceTypeDescriptor deviceTypeDescriptor : databaseContent.deviceTypeDescriptors) {
                for (FirmwareDescriptor firmwareDescriptor : databaseContent.firmwareDescriptors) {
                    if (firmwareDescriptor.getVersion().equals(deviceTypeDescriptor.getFirmwareVersion())) {
                        deviceTypeDescriptor.setFirmwareDescriptor(firmwareDescriptor);
                    }
                }
            }

            // Set firmware versions and applications for device personalities
            for (DevicePersonalityDescriptor devicePersonalityDescriptor : databaseContent.devicePersonalityDescriptors) {
                for (FirmwareDescriptor firmwareDescriptor : databaseContent.firmwareDescriptors) {
                    if (firmwareDescriptor.getVersion().equals(devicePersonalityDescriptor.getFirmwareVersion())) {
                        devicePersonalityDescriptor.setFirmwareDescriptor(firmwareDescriptor);
                    }
                }

                for (ApplicationDescriptor application : databaseContent.applicationDescriptors) {
                    if (application.getId() == devicePersonalityDescriptor.getApplicationId()) {
                        devicePersonalityDescriptor.setApplication(application);
                    }
                }
            }

            return databaseContent.productDescriptors;
        } catch (Exception e) {
            throw new RuntimeException("Failed to read from InputStream", e);
        }
    }
}
