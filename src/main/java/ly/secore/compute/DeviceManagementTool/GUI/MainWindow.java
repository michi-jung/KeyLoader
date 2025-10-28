package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.Cursor;
import javax.swing.JComponent;
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
        installBusyGlassPane();

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

    private void installBusyGlassPane() {
        JComponent glass = new JComponent() {
            @Override public boolean isOpaque() { return false; }
        };

        // Empty listeners ensure mouse events terminate at the glass pane
        java.awt.event.MouseAdapter eater = new java.awt.event.MouseAdapter() {};
        glass.addMouseListener(eater);
        glass.addMouseMotionListener(eater);
        glass.addMouseWheelListener(eater);

        setGlassPane(glass);            // attach to this JFrame
        glass.setVisible(false);        // start hidden
    }

    public void showBusyOverlay(boolean busy) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showBusyOverlay(busy));
            return;
        }

        JComponent glass = (JComponent)getGlassPane();
        glass.setCursor(Cursor.getPredefinedCursor(
            busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        glass.setVisible(busy);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        sidebar.setEnabled(enabled);
        lifecycleInformationPanel.setEnabled(enabled);
    }
}
