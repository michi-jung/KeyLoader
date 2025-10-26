package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.Cursor;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;

public class MainWindow extends JFrame {
    private Sidebar sidebar;
    private LifecycleInformationPanel lifecycleInformationPanel;
    private JSplitPane split;

    public MainWindow(EventBus eventBus) {
        super("compute secore.ly® Device Management Tool");

        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        sidebar = new Sidebar(eventBus);
        lifecycleInformationPanel = new LifecycleInformationPanel(eventBus);

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, lifecycleInformationPanel);
        split.setResizeWeight(0);               // keep right side size when resizing
        split.setContinuousLayout(true);        // smoother dragging
        split.setDividerSize(6);                // thinner divider
        split.setBorder(null);

        // Initial divider location equals sidebar preferred width
        split.setDividerLocation(sidebar.getPreferredSize().width);

        add(split);
        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    public void signalBusy() {
        if (SwingUtilities.isEventDispatchThread()) {
            sidebar.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            SwingUtilities.invokeLater(() -> sidebar.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
        }
    }

    public void signalReady() {
        if (SwingUtilities.isEventDispatchThread()) {
            sidebar.setCursor(Cursor.getDefaultCursor());
        } else {
            SwingUtilities.invokeLater(() -> sidebar.setCursor(Cursor.getDefaultCursor()));
        }
    }
}
