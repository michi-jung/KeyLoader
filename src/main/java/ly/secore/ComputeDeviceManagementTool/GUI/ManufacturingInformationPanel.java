package ly.secore.ComputeDeviceManagementTool.GUI;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import ly.secore.compute.ComputeDevice;
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

    public void setManufacturingInfo(ComputeDevice.ManufacturingInfo manufacturingInfo)
    {
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Manufacturing Information"));

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

        SwingUtilities.windowForComponent(this).pack();
    }

    private void initComponents() {
        JLabel tempLabel;
        Font plainLabelFont = UIManager.getFont("Label.font").deriveFont(Font.PLAIN);

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        tempLabel = new JLabel("Device Class");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        deviceClassLabel= new JLabel();
        deviceClassLabel.setFont(plainLabelFont);
        this.add(deviceClassLabel, "wrap,growx");

        tempLabel = new JLabel("Device Type");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        deviceTypeLabel = new JLabel();
        deviceTypeLabel.setFont(plainLabelFont);
        this.add(deviceTypeLabel, "wrap,growx");

        tempLabel = new JLabel("Engineering Change Level");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        engineeringChangeLevelLabel = new JLabel();
        engineeringChangeLevelLabel.setFont(plainLabelFont);
        this.add(engineeringChangeLevelLabel, "wrap,growx");

        tempLabel = new JLabel("Serial Number");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        serialNumberLabel = new JLabel();
        serialNumberLabel.setFont(plainLabelFont);
        this.add(serialNumberLabel, "wrap,growx");

        tempLabel = new JLabel("MAC Address");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        macAddressLabel = new JLabel();
        macAddressLabel.setFont(plainLabelFont);
        this.add(macAddressLabel, "wrap,growx");

        tempLabel = new JLabel("Time of Production");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        timeOfProductionLabel = new JLabel();
        timeOfProductionLabel.setFont(plainLabelFont);
        this.add(timeOfProductionLabel, "wrap,growx");
    }
}
