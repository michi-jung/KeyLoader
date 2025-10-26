package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventListener;
import java.util.EventObject;

public interface Listener extends EventListener {
    public void actionRequested(EventObject event);
}
