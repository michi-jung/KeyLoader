package ly.secore.compute.DeviceManagementTool.GUI;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import ly.secore.compute.Device;
import net.miginfocom.swing.MigLayout;


public class ManufacturingInformationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel deviceClassLabel;
    private JLabel deviceTypeLabel;
    private JLabel engineeringChangeLevelLabel;
    private JLabel serialNumberLabel;
    private JLabel macAddressLabel;
    private JLabel timeOfProductionLabel;

    public ManufacturingInformationPanel()
    {
        initComponents();
    }

    public void setManufacturingInfo(Device.ManufacturingInfo manufacturingInfo)
    {
        if (manufacturingInfo == null) {
            deviceClassLabel.setText("N/A");
            deviceTypeLabel.setText("N/A");
            engineeringChangeLevelLabel.setText("N/A");
            serialNumberLabel.setText("N/A");
            macAddressLabel.setText("N/A");
            timeOfProductionLabel.setText("N/A");
        } else {
            deviceClassLabel.setText(manufacturingInfo.getDeviceClassName() +
                                        " (" + manufacturingInfo.getDeviceClassUUID() + ")");

            deviceTypeLabel.setText(manufacturingInfo.getDeviceTypeName() +
                                    " (" + manufacturingInfo.getDeviceType() + ")");
            engineeringChangeLevelLabel.setText(
                String.valueOf(manufacturingInfo.getEngineeringChangeLevel()));

            serialNumberLabel.setText(String.format("%010d", manufacturingInfo.getSerialNumber()));

            macAddressLabel.setText(HexFormat.ofDelimiter(":").withUpperCase()
                .formatHex(manufacturingInfo.getMACAddress()));

            timeOfProductionLabel.setText(manufacturingInfo.getTimeOfProduction()
                .format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.FULL)));
        }

        SwingUtilities.windowForComponent(this).pack();
    }

    private void initComponents() {
        JLabel tempLabel;

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Manufacturing Information"));

        tempLabel = new JLabel("Device Class");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        deviceClassLabel= new JLabel("N/A");
        this.add(deviceClassLabel, "wrap,growx");

        tempLabel = new JLabel("Device Type");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        deviceTypeLabel = new JLabel("N/A");
        this.add(deviceTypeLabel, "wrap,growx");

        tempLabel = new JLabel("Engineering Change Level");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        engineeringChangeLevelLabel = new JLabel("");
        this.add(engineeringChangeLevelLabel, "wrap,growx");

        tempLabel = new JLabel("Serial Number");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        serialNumberLabel = new JLabel("N/A");
        this.add(serialNumberLabel, "wrap,growx");

        tempLabel = new JLabel("MAC Address");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        macAddressLabel = new JLabel("N/A");
        this.add(macAddressLabel, "wrap,growx");

        tempLabel = new JLabel("Time of Production");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        timeOfProductionLabel = new JLabel("N/A");
        this.add(timeOfProductionLabel, "wrap,growx");
    }
}
