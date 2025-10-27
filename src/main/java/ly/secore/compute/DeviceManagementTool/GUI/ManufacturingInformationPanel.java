package ly.secore.compute.DeviceManagementTool.GUI;

import java.time.format.DateTimeFormatter;
import java.util.EventObject;
import java.util.HexFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.Listener;
import ly.secore.compute.DeviceManagementTool.Event.UpdateDeviceInformationRequested;
import net.miginfocom.swing.MigLayout;


public class ManufacturingInformationPanel extends JPanel implements Listener {
    private static final long serialVersionUID = 1L;

    private JLabel deviceClassLabel;
    private JLabel deviceClassValue;
    private JLabel deviceTypeLabel;
    private JLabel deviceTypeValue;
    private JLabel engineeringChangeLevelLabel;
    private JLabel engineeringChangeLevelValue;
    private JLabel serialNumberLabel;
    private JLabel serialNumberValue;
    private JLabel macAddressLabel;
    private JLabel macAddressValue;
    private JLabel timeOfProductionLabel;
    private JLabel timeOfProductionValue;

    public ManufacturingInformationPanel(EventBus eventBus)
    {
        eventBus.addListener(this);
        initComponents();
    }

    public void setManufacturingInfo(Device.ManufacturingInfo manufacturingInfo)
    {
        if (manufacturingInfo == null) {
            deviceClassValue.setText("N/A");
            deviceTypeValue.setText("N/A");
            engineeringChangeLevelValue.setText("N/A");
            serialNumberValue.setText("N/A");
            macAddressValue.setText("N/A");
            timeOfProductionValue.setText("N/A");
            setEnabled(false);
        } else {
            deviceClassValue.setText(manufacturingInfo.getDeviceClassName() +
                                        " (" + manufacturingInfo.getDeviceClassUUID() + ")");

            deviceTypeValue.setText(manufacturingInfo.getDeviceTypeName() +
                                    " (" + manufacturingInfo.getDeviceType() + ")");
            engineeringChangeLevelValue.setText(
                String.valueOf(manufacturingInfo.getEngineeringChangeLevel()));

            serialNumberValue.setText(String.format("%010d", manufacturingInfo.getSerialNumber()));

            macAddressValue.setText(HexFormat.ofDelimiter(":").withUpperCase()
                .formatHex(manufacturingInfo.getMACAddress()));

            timeOfProductionValue.setText(manufacturingInfo.getTimeOfProduction()
                .format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.FULL)));

            setEnabled(true);
        }

        SwingUtilities.windowForComponent(this).pack();
    }

    public void actionRequested(EventObject event) {
        if (event instanceof UpdateDeviceInformationRequested) {
            UpdateDeviceInformationRequested updateEvent = (UpdateDeviceInformationRequested)event;
            setManufacturingInfo(updateEvent.getDeviceInformation().getManufacturingInfo());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        deviceClassLabel.setEnabled(enabled);
        deviceClassValue.setEnabled(enabled);
        deviceTypeLabel.setEnabled(enabled);
        deviceTypeValue.setEnabled(enabled);
        engineeringChangeLevelLabel.setEnabled(enabled);
        engineeringChangeLevelValue.setEnabled(enabled);
        serialNumberLabel.setEnabled(enabled);
        serialNumberValue.setEnabled(enabled);
        macAddressLabel.setEnabled(enabled);
        macAddressValue.setEnabled(enabled);
        timeOfProductionLabel.setEnabled(enabled);
        timeOfProductionValue.setEnabled(enabled);
    }

    private void initComponents() {
        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Manufacturing Information"));

        deviceClassLabel = new JLabel("Device Class");
        deviceClassLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(deviceClassLabel, "growx");

        deviceClassValue= new JLabel("N/A");
        this.add(deviceClassValue, "wrap,growx");

        deviceTypeLabel = new JLabel("Device Type");
        deviceTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(deviceTypeLabel, "growx");

        deviceTypeValue = new JLabel("N/A");
        this.add(deviceTypeValue, "wrap,growx");

        engineeringChangeLevelLabel = new JLabel("Engineering Change Level");
        engineeringChangeLevelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(engineeringChangeLevelLabel, "growx");

        engineeringChangeLevelValue = new JLabel("");
        this.add(engineeringChangeLevelValue, "wrap,growx");

        serialNumberLabel = new JLabel("Serial Number");
        serialNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(serialNumberLabel, "growx");

        serialNumberValue = new JLabel("N/A");
        this.add(serialNumberValue, "wrap,growx");

        macAddressLabel = new JLabel("MAC Address");
        macAddressLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(macAddressLabel, "growx");

        macAddressValue = new JLabel("N/A");
        this.add(macAddressValue, "wrap,growx");

        timeOfProductionLabel = new JLabel("Time of Production");
        timeOfProductionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(timeOfProductionLabel, "growx");

        timeOfProductionValue = new JLabel("N/A");
        this.add(timeOfProductionValue, "wrap,growx");
    }
}
