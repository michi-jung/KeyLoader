package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventObject;

public class FactoryFlashRequested extends EventObject {
    private static final long serialVersionUID = 1L;
    private final String initialFileName;

    public FactoryFlashRequested(Object source, String initialFileName) {
        super(source);
        this.initialFileName = initialFileName;
    }

    public String getInitialFileName() {
        return initialFileName;
    }
}
