package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventObject;
import ly.secore.compute.DeviceManagementTool.DataModel.DeviceInformation;

public class UpdateDeviceInformationRequested extends EventObject {
    private static final long serialVersionUID = 1L;
    private DeviceInformation deviceInformation;

    public UpdateDeviceInformationRequested(Object source, DeviceInformation deviceInformation)
    {
        super(source);
        this.deviceInformation = deviceInformation;
    }

    public DeviceInformation getDeviceInformation() {
        return deviceInformation;
    }
}
