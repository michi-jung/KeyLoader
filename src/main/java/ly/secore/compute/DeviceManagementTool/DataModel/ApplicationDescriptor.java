package ly.secore.compute.DeviceManagementTool.DataModel;

public class ApplicationDescriptor {
    private int id;
    private String applicationClass;
    private String version;
    private String description;
    private String fileName;
    private String appKeyLabel;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setApplicationClass(String applicationClass) {
        this.applicationClass = applicationClass;
    }

    public String getApplicationClass() {
        return applicationClass;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setAppKeyLabel(String appKeyLabel) {
        this.appKeyLabel = appKeyLabel;
    }

    public String getAppKeyLabel() {
        return appKeyLabel;
    }

    @Override
    public String toString() {
        return "ApplicationDescriptor{" +
                "id=" + id + '\'' +
                ", applicationClass='" + applicationClass + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", fileName='" + fileName + '\'' +
                ", appKeyLabel='" + appKeyLabel + '\'' +
                '}';
    }
}
