package ly.secore.ComputeDeviceManagementTool.DataModel;

public class FirmwareDescriptor {
    private String deviceClass;
    private String version;
    private String fileName;
    private String initialFileName;

    public void setDeviceClass(String deviceClass) {
        this.deviceClass = deviceClass;
    }

    public String getDeviceClass() {
        return deviceClass;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setInitialFileName(String initialFileName) {
        this.initialFileName = initialFileName;
    }

    public String getInitialFileName() {
        return initialFileName;
    }

    @Override
    public String toString() {
        return "FirmwareDescriptor{" +
                "deviceClass='" + deviceClass + '\'' +
                ", version='" + version + '\'' +
                ", fileName='" + fileName + '\'' +
                ", initialFileName='" + initialFileName + '\'' +
                '}';
    }
}
