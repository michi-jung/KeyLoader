package ly.secore.compute.DeviceManagementTool.Event;

import java.nio.file.Path;
import java.util.EventObject;

public class ConnectToDeviceRequested extends EventObject {
    private static final long serialVersionUID = 1L;
    private final Path uartPath;

    public ConnectToDeviceRequested(Object source, Path uartPath) {
        super(source);
        this.uartPath = uartPath;
    }

    public Path getUartPath() {
        return uartPath;
    }
}