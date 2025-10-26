package ly.secore.compute.DeviceManagementTool;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EventListener;
import java.util.EventObject;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.GUI.MainWindow;

/**
 * Main application class for displaying manufacturing and incarnation information.
 */

public class Application implements EventListener {

    private MainWindow mainWindow;
    private Device computeDevice;

    public Application() {
        mainWindow = new MainWindow(this);
    }

    public void actionRequested(EventObject requestEvent)
    {
        try {
            if (requestEvent instanceof ConnectToDeviceRequested) {
                ConnectToDeviceRequested connectRequest = (ConnectToDeviceRequested) requestEvent;
                Path uartPath = connectRequest.getUartPath();

                computeDevice = new Device(uartPath.toString());
                computeDevice.openServiceSession();

                mainWindow.setManufacturingInfo(computeDevice.getManufacturingInfo());
                mainWindow.setIncarnationInfo(computeDevice.getReincarnationInfo());
                mainWindow.setDDM885Info(computeDevice.getDDM885Info());
            }
            else {
                throw new RuntimeException(
                    "actionRequested() failed",
                    new IllegalArgumentException("Unknown request event: " + requestEvent));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to process action request", e);
        }
    }

    public Path[] getUartPaths() {
        try (Stream<Path> stream = Files.find(Paths.get("/dev"),
                                              1,
                                              (path, attrs) -> {
                                                  return path.toString().startsWith("/dev/ttyDDM-");
                                              })) {
            return stream.collect(Collectors.toList()).toArray(new Path[0]);
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain list of /dev/ttyDDM-* devices", e);
        }
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            new Application();
        });
    }

    public static class ConnectToDeviceRequested extends EventObject {
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
}
