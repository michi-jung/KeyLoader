package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import ly.secore.compute.DeviceManagementTool.DataModel.DeviceInformation;
import ly.secore.compute.DeviceManagementTool.Event.Listener;
import ly.secore.compute.DeviceManagementTool.Event.UpdateDeviceInformationRequested;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.UpdateUartPathsRequested;

public class Sidebar extends JPanel implements ActionListener, Listener {
    private static final long serialVersionUID = 1L;
    private final EventBus eventBus;
    private DeviceInformation deviceInformation = new DeviceInformation();
    private JComboBox<Path> uartComboBox = new JComboBox<>();
    private JButton connectButton = new JButton("Connect to Device");
    private ManufacturingInformationPanel manufacturingInformationPanel = new ManufacturingInformationPanel();
    private IncarnationInformationPanel incarnationInformationPanel = new IncarnationInformationPanel();
    private DDM885InformationPanel ddm885InformationPanel = new DDM885InformationPanel();

    public Sidebar(EventBus eventBus) {
        GridBagConstraints gbc = new GridBagConstraints();

        this.eventBus = eventBus;

        connectButton.addActionListener(this);
        eventBus.addListener(this);

        setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;   // expand horizontally
        gbc.weighty = 0.0;   // no vertical expansion
        gbc.insets = new Insets(5, 10, 5, 10);

        add(uartComboBox, gbc);
        add(connectButton, gbc);
        add(manufacturingInformationPanel, gbc);
        add(incarnationInformationPanel, gbc);
        add(ddm885InformationPanel, gbc);

        gbc.weighty = 1.0;
        add(new JPanel(), gbc);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == connectButton) {
            if (deviceInformation.isDeviceConnected()) {
                eventBus.disconnectFromDevice(e.getSource());
            } else {
                eventBus.connectToDevice(e.getSource(), (Path)uartComboBox.getSelectedItem());
            }
        }
    }

    public void actionRequested(EventObject requestEvent) {
        if (requestEvent instanceof UpdateUartPathsRequested) {
            uartComboBox.removeAllItems();
            for (Path path : ((UpdateUartPathsRequested)requestEvent).getUartPaths()) {
                uartComboBox.addItem(path);
            }
            SwingUtilities.windowForComponent(this).pack();
        } else if (requestEvent instanceof UpdateDeviceInformationRequested) {
            deviceInformation =
                ((UpdateDeviceInformationRequested)requestEvent).getDeviceInformation();

            if (deviceInformation.isDeviceConnected()) {
                connectButton.setText("Disconnect from Device");
                uartComboBox.setEnabled(false);
                manufacturingInformationPanel.setEnabled(true);
                incarnationInformationPanel.setEnabled(true);
                ddm885InformationPanel.setEnabled(true);
            } else {
                connectButton.setText("Connect to Device");
                uartComboBox.setEnabled(true);
                manufacturingInformationPanel.setEnabled(false);
                incarnationInformationPanel.setEnabled(false);
                ddm885InformationPanel.setEnabled(false);
            }

            manufacturingInformationPanel.setManufacturingInfo(deviceInformation.getManufacturingInfo());
            incarnationInformationPanel.setIncarnationInfo(deviceInformation.getReincarnationInfo());
            ddm885InformationPanel.setDDM885Info(deviceInformation.getDDM885Info());
        }
    }
}
