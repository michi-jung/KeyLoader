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
import ly.secore.compute.Device;

public class Sidebar extends JPanel implements ActionListener, Listener {
    private static final long serialVersionUID = 1L;
    private final EventBus eventBus;
    private DeviceInformation deviceInformation = new DeviceInformation();
    private JComboBox<Path> uartComboBox = new JComboBox<>();
    private JButton connectButton = new JButton("Connect to Device");
    private JButton personalizeButton = new JButton("Factory Flash, Test and Personalize");
    private JButton factoryFlashButton = new JButton("Factory Flash");
    private JButton firmwareUpdateButton = new JButton("Firmware Update");
    private JButton applicationUpdateButton = new JButton("Application Update");
    private VitalProductDataPanel vitalProductDataPanel;

    public Sidebar(EventBus eventBus) {
        GridBagConstraints gbc = new GridBagConstraints();

        this.eventBus = eventBus;

        vitalProductDataPanel = new VitalProductDataPanel(eventBus);
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
        add(personalizeButton, gbc);
        add(factoryFlashButton, gbc);
        add(firmwareUpdateButton, gbc);
        add(applicationUpdateButton, gbc);
        add(vitalProductDataPanel, gbc);

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
            } else {
                connectButton.setText("Connect to Device");
                uartComboBox.setEnabled(true);
            }

            personalizeButton.setEnabled(false);
            factoryFlashButton.setEnabled(false);
            firmwareUpdateButton.setEnabled(false);
            applicationUpdateButton.setEnabled(false);

            if (deviceInformation.getLifecycleInfo() != null)
            {
                switch (deviceInformation.getLifecycleInfo().state)
                {
                    case Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURED:
                        personalizeButton.setEnabled(true);
                        factoryFlashButton.setEnabled(true);
                        break;
                    case Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURING_TEST:
                        firmwareUpdateButton.setEnabled(true);
                        break;
                    case Device.LifecycleInfo.LIFECYCLE_STATE_PERSONALIZATION:
                        firmwareUpdateButton.setEnabled(true);
                        break;
                    case Device.LifecycleInfo.LIFECYCLE_STATE_OPERATION:
                        firmwareUpdateButton.setEnabled(true);
                        applicationUpdateButton.setEnabled(true);
                        break;
                    case Device.LifecycleInfo.LIFECYCLE_STATE_FORENSIC_ANALYSIS:
                        firmwareUpdateButton.setEnabled(true);
                        break;
                    case Device.LifecycleInfo.LIFECYCLE_STATE_DECOMMISSIONED:
                    default:
                        break;
                }
            }
        }
    }
}
