package ly.secore.compute.DeviceManagementTool;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EventObject;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.DataModel.DeviceInformation;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.ConnectToDeviceRequested;
import ly.secore.compute.DeviceManagementTool.Event.DisconnectFromDeviceRequested;
import ly.secore.compute.DeviceManagementTool.Event.Listener;
import ly.secore.compute.DeviceManagementTool.GUI.MainWindow;

/**
 * Main application class for displaying manufacturing and incarnation information.
 */

public class Application implements Listener {

    private MainWindow mainWindow;
    private Device computeDevice;
    private DeviceInformation deviceInformation = new DeviceInformation();
    private EventBus eventBus = new EventBus();

    public Application() {
        eventBus = new EventBus();
        eventBus.addListener(this);
        mainWindow = new MainWindow(eventBus);
        eventBus.updateUartPaths(this, getUartPaths());
        eventBus.updateDeviceInformation(this, deviceInformation);
        mainWindow.setVisible(true);
    }

    public void actionRequested(EventObject requestEvent)
    {
        try {
            if (requestEvent instanceof ConnectToDeviceRequested) {
                ConnectToDeviceRequested connectRequest = (ConnectToDeviceRequested) requestEvent;
                Path uartPath = connectRequest.getUartPath();

                mainWindow.signalBusy();

                computeDevice = new Device(uartPath.toString());
                computeDevice.openServiceSession();

                deviceInformation.setDeviceConnected(true);
                deviceInformation.setManufacturingInfo(computeDevice.getManufacturingInfo());
                deviceInformation.setReincarnationInfo(computeDevice.getReincarnationInfo());
                deviceInformation.setDDM885Info(computeDevice.getDDM885Info());

                eventBus.updateDeviceInformation(this, deviceInformation);

                mainWindow.signalReady();
            } else if (requestEvent instanceof DisconnectFromDeviceRequested) {
                if (computeDevice != null) {
                    computeDevice.closeServiceSession();
                    computeDevice = null;
                }

                deviceInformation.setDeviceConnected(false);
                deviceInformation.setManufacturingInfo(null);
                deviceInformation.setReincarnationInfo(null);
                deviceInformation.setDDM885Info(null);

                eventBus.updateDeviceInformation(this, deviceInformation);
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
}
