package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;

public class VitalProductDataPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public VitalProductDataPanel(EventBus eventBus) {
        initComponents(eventBus);
    }

    private void initComponents(EventBus eventBus) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;   // expand horizontally
        gbc.weighty = 0.0;   // no vertical expansion
        gbc.insets = new Insets(5, 10, 5, 10);

        setLayout(new GridBagLayout());
        setBorder(javax.swing.BorderFactory.createTitledBorder(
                  javax.swing.BorderFactory.createEtchedBorder(), "Vital Product Data"));

        add(new ManufacturingInformationPanel(eventBus), gbc);
        add(new DDM885InformationPanel(eventBus), gbc);
        add(new IncarnationInformationPanel(eventBus), gbc);
    }
}