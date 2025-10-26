package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import ly.secore.compute.DeviceManagementTool.Application;
import ly.secore.compute.Device;

public class Sidebar extends JPanel implements ActionListener{
    private static final long serialVersionUID = 1L;
    private final Application application;
    private JComboBox<Path> uartComboBox;
    private JButton connectButton = new JButton("Connect to Device");
    private ManufacturingInformationPanel manufacturingInformationPanel = new ManufacturingInformationPanel();
    private IncarnationInformationPanel incarnationInformationPanel = new IncarnationInformationPanel();
    private DDM885InformationPanel ddm885InformationPanel = new DDM885InformationPanel();

    public Sidebar(Application application) {
        GridBagConstraints gbc = new GridBagConstraints();

        this.application = application;

        uartComboBox = new JComboBox<>(application.getUartPaths());
        connectButton.addActionListener(this);

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
            Path uartPath = (Path) uartComboBox.getSelectedItem();
            if (uartPath != null) {
                application.actionRequested(
                    new Application.ConnectToDeviceRequested(this, uartPath));
            }
        }
    }

    public void setManufacturingInfo(Device.ManufacturingInfo manufacturingInfo) {
        manufacturingInformationPanel.setManufacturingInfo(manufacturingInfo);
    }

    public void setIncarnationInfo(Device.ReincarnationInfo reincarnationInfo) {
        incarnationInformationPanel.setIncarnationInfo(reincarnationInfo);
    }

    public void setDDM885Info(Device.DDM885Info ddm885Info) {
        ddm885InformationPanel.setDDM885Info(ddm885Info);
    }
}
