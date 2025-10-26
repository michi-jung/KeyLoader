package ly.secore.compute.DeviceManagementTool.GUI;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.Application;

public class MainWindow extends JFrame {
    private Sidebar sidebar;

    public MainWindow(Application application) {
        JFrame frame = new JFrame("compute secore.ly® Device Management Tool");

        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        sidebar = new Sidebar(application);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, new LifecycleInformationPanel());
        split.setResizeWeight(0);               // keep right side size when resizing
        split.setContinuousLayout(true);        // smoother dragging
        split.setDividerSize(6);                // thinner divider
        split.setBorder(null);

        // Initial divider location equals sidebar preferred width
        split.setDividerLocation(sidebar.getPreferredSize().width);

        frame.add(split);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }

    public void setManufacturingInfo(Device.ManufacturingInfo manufacturingInfo) {
        sidebar.setManufacturingInfo(manufacturingInfo);
    }

    public void setIncarnationInfo(Device.ReincarnationInfo reincarnationInfo) {
        sidebar.setIncarnationInfo(reincarnationInfo);
    }

    public void setDDM885Info(Device.DDM885Info ddm885Info) {
        sidebar.setDDM885Info(ddm885Info);
    }
}
