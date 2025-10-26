package ly.secore.compute.DeviceManagementTool.Event;

import java.nio.file.Path;
import java.util.EventObject;

public class UpdateUartPathsRequested extends EventObject {
    private static final long serialVersionUID = 1L;
    private final Path[] uartPaths;

    public UpdateUartPathsRequested(Object source, Path[] uartPaths) {
        super(source);
        this.uartPaths = uartPaths;
    }

    public Path[] getUartPaths() {
        return uartPaths;
    }
}