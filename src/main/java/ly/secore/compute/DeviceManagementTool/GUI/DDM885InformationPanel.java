package ly.secore.compute.DeviceManagementTool.GUI;

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

public class DDM885InformationPanel extends JPanel implements Listener {
    private static final long serialVersionUID = 1L;

    private JLabel productKeyLabel;
    private JLabel productKeyValue;
    private JLabel orderIdLabel;
    private JLabel orderIdValue;

    public DDM885InformationPanel(EventBus eventBus) {
        eventBus.addListener(this);
        initComponents();
    }

    public void setDDM885Info(Device.DDM885Info ddm885Info)
    {
        if (ddm885Info == null) {
            productKeyValue.setText("N/A");
            orderIdValue.setText("N/A");
            setEnabled(false);
        } else {
            orderIdValue.setText(String.format("%d", ddm885Info.orderId));
            productKeyValue.setText(ddm885Info.productKey);
            setEnabled(true);
        }

        SwingUtilities.windowForComponent(this).pack();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        productKeyLabel.setEnabled(enabled);
        productKeyValue.setEnabled(enabled);
        orderIdLabel.setEnabled(enabled);
        orderIdValue.setEnabled(enabled);
    }

    public void actionRequested(EventObject event) {
        if (event instanceof UpdateDeviceInformationRequested) {
            UpdateDeviceInformationRequested updateEvent = (UpdateDeviceInformationRequested)event;
            setDDM885Info(updateEvent.getDeviceInformation().getDDM885Info());
        }
    }

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(),
                  "DDM 885 Specific Manufacturing Information"));

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        productKeyLabel = new JLabel("Product Key");
        productKeyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(productKeyLabel, "growx");

        productKeyValue = new JLabel("N/A");
        this.add(productKeyValue, "wrap,growx");

        orderIdLabel = new JLabel("Order ID");
        orderIdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(orderIdLabel, "growx");

        orderIdValue = new JLabel("N/A");
        this.add(orderIdValue, "wrap,growx");
    }
}
