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
import javax.swing.SwingWorker;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.DataModel.DeviceInformation;
import ly.secore.compute.DeviceManagementTool.DataModel.JsonReader;
import ly.secore.compute.DeviceManagementTool.DataModel.ProductDescriptor;
import ly.secore.compute.DeviceManagementTool.Event.ConnectToDeviceRequested;
import ly.secore.compute.DeviceManagementTool.Event.DisconnectFromDeviceRequested;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.FactoryFlashRequested;
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
    private String uartPath;

    public Application() {
        eventBus = new EventBus();
        eventBus.addListener(this);
        mainWindow = new MainWindow(eventBus);
        eventBus.updateUartPaths(this, getUartPaths());
        eventBus.updateProductDescriptors(this, getProductDescriptors());
        eventBus.updateDeviceInformation(this, deviceInformation);
        mainWindow.setVisible(true);
    }

    protected void connectToDevice() {
        if (computeDevice != null) {
            throw new IllegalStateException("Already connected to a device");
        }

        try {
            computeDevice = new Device(uartPath);
            computeDevice.openServiceSession(1);
            deviceInformation.setDeviceConnected(true);
            deviceInformation.setManufacturingInfo(computeDevice.getManufacturingInfo());
            deviceInformation.setReincarnationInfo(computeDevice.getReincarnationInfo());
            deviceInformation.setDDM885Info(computeDevice.getDDM885Info());
            deviceInformation.setLifecycleInfo(computeDevice.getLifecycleInfo());
        } catch (IOException e) {
            if (computeDevice != null) {
                computeDevice.close();
                computeDevice = null;
            }
            try {
                computeDevice = new Device(uartPath);
                deviceInformation.setDeviceConnected(true);
                deviceInformation.setManufacturingInfo(null);
                deviceInformation.setReincarnationInfo(null);
                deviceInformation.setDDM885Info(null);
                deviceInformation.setLifecycleInfo(new Device.LifecycleInfo(
                    Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURED));
            } catch (IOException e2) {
                computeDevice = null;
                deviceInformation.setDeviceConnected(false);
            }
        }
    }

    public void actionRequested(EventObject requestEvent)
    {
        try {
            if (requestEvent instanceof ConnectToDeviceRequested) {
                ConnectToDeviceRequested connectRequest = (ConnectToDeviceRequested) requestEvent;
                uartPath = connectRequest.getUartPath().toString();

                mainWindow.showBusyOverlay(true);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        connectToDevice();
                        return null;
                    }

                    @Override
                    protected void done() {
                        System.out.println("Connected to device: " + (deviceInformation.isDeviceConnected() ? "Yes" : "No"));
                        eventBus.updateDeviceInformation(this, deviceInformation);
                        mainWindow.showBusyOverlay(false);
                    }
                };

                worker.execute();
            } else if (requestEvent instanceof DisconnectFromDeviceRequested) {
                if (computeDevice != null &&
                    deviceInformation.getLifecycleInfo().state !=
                        Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURED)
                {
                    computeDevice.closeServiceSession();
                }

                computeDevice.close();
                computeDevice = null;

                deviceInformation.setDeviceConnected(false);
                deviceInformation.setManufacturingInfo(null);
                deviceInformation.setReincarnationInfo(null);
                deviceInformation.setDDM885Info(null);
                deviceInformation.setLifecycleInfo(null);

                eventBus.updateDeviceInformation(this, deviceInformation);
            } else if (requestEvent instanceof FactoryFlashRequested) {
                mainWindow.showBusyOverlay(true);

                SwingWorker<Void, Void> worker = new SwingWorker<>() {
                    @Override
                    protected Void doInBackground() {
                        FactoryFlashRequested factoryFlashRequest =
                            (FactoryFlashRequested)requestEvent;

                        try {
                            computeDevice.factoryFlash(getClass()
                                .getResourceAsStream(factoryFlashRequest.getInitialFileName()));
                        } catch (IOException e) {
                            System.err.println("Factory flashing failed: " + e.getMessage());
                        }

                        computeDevice.close();
                        computeDevice = null;

                        return null;
                    }

                    @Override
                    protected void done() {
                        connectToDevice();
                        eventBus.updateDeviceInformation(this, deviceInformation);
                        mainWindow.showBusyOverlay(false);
                    }
                };

                worker.execute();
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

    public ProductDescriptor[] getProductDescriptors() {
        return JsonReader.getProductDescriptors(getClass()
            .getResourceAsStream("product-descriptors.json")).toArray(new ProductDescriptor[0]);
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            new Application();
        });
    }
}
