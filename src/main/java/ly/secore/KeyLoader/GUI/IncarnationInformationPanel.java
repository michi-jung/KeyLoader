package ly.secore.KeyLoader.GUI;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.time.format.DateTimeFormatter;
import ly.secore.compute.ComputeDevice;
import net.miginfocom.swing.MigLayout;

public class IncarnationInformationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel timeOfReincarnationLabel;
    private JLabel devicePersonalityLabel;
    private JLabel operatingModeLabel;
    private JLabel masterKeyIdLabel;

    public IncarnationInformationPanel()
    {
        initComponents();
    }

    public void setIncarnationInfo(ComputeDevice.ReincarnationInfo reincarnationInfo)
    {
        timeOfReincarnationLabel.setText(reincarnationInfo.getTimeOfReincarnation()
            .format(DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.FULL)));

        devicePersonalityLabel.setText(reincarnationInfo.getDevicePersonalityName()
            + " (" + reincarnationInfo.getDevicePersonality() + ")");

        operatingModeLabel.setText(reincarnationInfo.getOperatingModeName()
            + " (" + reincarnationInfo.getOperatingMode() + ")");

        masterKeyIdLabel.setText(reincarnationInfo.getMasterKeyIdName());

        SwingUtilities.windowForComponent(this).pack();
    }

    private void initComponents() {
        JLabel tempLabel;

        setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(), "Incarnation Information"));

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        tempLabel = new JLabel("Time of Reincarnation");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        timeOfReincarnationLabel= new JLabel();
        this.add(timeOfReincarnationLabel, "wrap,growx");

        tempLabel = new JLabel("Device Personality");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        devicePersonalityLabel = new JLabel();
        this.add(devicePersonalityLabel, "wrap,growx");

        tempLabel = new JLabel("Operating Mode");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        operatingModeLabel = new JLabel();
        this.add(operatingModeLabel, "wrap,growx");

        tempLabel = new JLabel("Master Key ID");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        masterKeyIdLabel = new JLabel();
        this.add(masterKeyIdLabel, "wrap,growx");
    }
}
