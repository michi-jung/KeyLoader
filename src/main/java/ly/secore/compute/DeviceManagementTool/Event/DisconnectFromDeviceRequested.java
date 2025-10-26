package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventObject;

public class DisconnectFromDeviceRequested extends EventObject {
    private static final long serialVersionUID = 1L;

    public DisconnectFromDeviceRequested(Object source) {
        super(source);
    }
}