package ly.secore.compute.DeviceManagementTool.GUI;

import java.time.format.DateTimeFormatter;
import java.util.EventObject;
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

public class IncarnationInformationPanel extends JPanel implements Listener{
    private static final long serialVersionUID = 1L;

    private JLabel timeOfReincarnationLabel;
    private JLabel timeOfReincarnationValue;
    private JLabel devicePersonalityLabel;
    private JLabel devicePersonalityValue;
    private JLabel operatingModeLabel;
    private JLabel operatingModeValue;
    private JLabel masterKeyIdLabel;
    private JLabel masterKeyIdValue;

    public IncarnationInformationPanel(EventBus eventBus)
    {
        eventBus.addListener(this);
        initComponents();
    }

    public void setIncarnationInfo(Device.ReincarnationInfo reincarnationInfo)
    {
        if (reincarnationInfo == null) {
            timeOfReincarnationValue.setText("N/A");
            devicePersonalityValue.setText("N/A");
            operatingModeValue.setText("N/A");
            masterKeyIdValue.setText("N/A");
            setEnabled(false);
        } else {
            timeOfReincarnationValue.setText(reincarnationInfo.getTimeOfReincarnation()
                .format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.FULL)));
            devicePersonalityValue.setText(
                Device.ReincarnationInfo
                    .getDevicePersonalityName(reincarnationInfo.getDevicePersonality()) +
                    " (" + reincarnationInfo.getDevicePersonality() + ")");
            operatingModeValue.setText(reincarnationInfo.getOperatingModeName()
                + " (" + reincarnationInfo.getOperatingMode() + ")");
            masterKeyIdValue.setText(reincarnationInfo.getMasterKeyIdName());
            setEnabled(true);
        }

        SwingUtilities.windowForComponent(this).pack();
    }

    public void actionRequested(EventObject event) {
        if (event instanceof UpdateDeviceInformationRequested) {
            UpdateDeviceInformationRequested updateEvent = (UpdateDeviceInformationRequested)event;
            setIncarnationInfo(updateEvent.getDeviceInformation().getReincarnationInfo());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        timeOfReincarnationLabel.setEnabled(enabled);
        timeOfReincarnationValue.setEnabled(enabled);
        devicePersonalityLabel.setEnabled(enabled);
        devicePersonalityValue.setEnabled(enabled);
        operatingModeLabel.setEnabled(enabled);
        operatingModeValue.setEnabled(enabled);
        masterKeyIdLabel.setEnabled(enabled);
        masterKeyIdValue.setEnabled(enabled);
    }

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(), "Incarnation Information"));

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        timeOfReincarnationLabel = new JLabel("Time of Reincarnation");
        timeOfReincarnationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(timeOfReincarnationLabel, "growx");

        timeOfReincarnationValue= new JLabel("N/A");
        this.add(timeOfReincarnationValue, "wrap,growx");

        devicePersonalityLabel = new JLabel("Device Personality");
        devicePersonalityLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(devicePersonalityLabel, "growx");

        devicePersonalityValue = new JLabel("N/A");
        this.add(devicePersonalityValue, "wrap,growx");

        operatingModeLabel = new JLabel("Operating Mode");
        operatingModeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(operatingModeLabel, "growx");

        operatingModeValue = new JLabel("N/A");
        this.add(operatingModeValue, "wrap,growx");

        masterKeyIdLabel = new JLabel("Master Key ID");
        masterKeyIdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(masterKeyIdLabel, "growx");

        masterKeyIdValue = new JLabel("N/A");
        this.add(masterKeyIdValue, "wrap,growx");
    }
}
