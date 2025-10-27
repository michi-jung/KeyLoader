package ly.secore.compute.DeviceManagementTool.Event;

import java.nio.file.Path;
import java.util.EventObject;
import javax.swing.event.EventListenerList;
import ly.secore.compute.DeviceManagementTool.DataModel.DeviceInformation;
import ly.secore.compute.DeviceManagementTool.DataModel.ProductDescriptor;

public class EventBus {
    private final EventListenerList listeners = new EventListenerList();

    public void addListener(Listener l) {
        listeners.add(Listener.class, l);
    }

    public void removeListener(Listener l) {
        listeners.remove(Listener.class, l);
    }

    public void connectToDevice(Object source, Path uartPath) {
        fireEvent(new ConnectToDeviceRequested(source, uartPath));
    }

    public void disconnectFromDevice(Object source) {
        fireEvent(new DisconnectFromDeviceRequested(source));
    }

    public void factoryFlash(Object source, String initialFileName) {
        fireEvent(new FactoryFlashRequested(source, initialFileName));
    }

    public void updateUartPaths(Object source, Path[] uartPaths) {
        fireEvent(new UpdateUartPathsRequested(source, uartPaths));
    }

    public void updateProductDescriptors(Object source, ProductDescriptor[] productDescriptors) {
        fireEvent(new UpdateProductDescriptors(source, productDescriptors));
    }

    public void updateDeviceInformation(Object source, DeviceInformation deviceInformation) {
        fireEvent(new UpdateDeviceInformationRequested(source, deviceInformation));
    }

    public void selectProductDescriptor(ProductDescriptor productDescriptor) {
        fireEvent(new SelectProductDescriptorRequested(this, productDescriptor));
    }

    private void fireEvent(EventObject event) {
        for (Listener l : listeners.getListeners(Listener.class)) {
            l.actionRequested(event);
        }
    }
}
