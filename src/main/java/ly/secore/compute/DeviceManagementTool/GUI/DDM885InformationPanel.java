package ly.secore.compute.DeviceManagementTool.GUI;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import ly.secore.compute.Device;
import net.miginfocom.swing.MigLayout;

public class DDM885InformationPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private JLabel productKeyLabel;
    private JLabel orderIdLabel;

    public DDM885InformationPanel()
    {
        initComponents();
    }

    public void setDDM885Info(Device.DDM885Info ddm885Info)
    {
        if (ddm885Info == null) {
            productKeyLabel.setText("N/A");
            orderIdLabel.setText("N/A");
            return;
        } else {
            orderIdLabel.setText(String.format("%d", ddm885Info.orderId));
            productKeyLabel.setText(ddm885Info.productKey);
        }

        SwingUtilities.windowForComponent(this).pack();
    }

    private void initComponents() {
        JLabel tempLabel;

        setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(), "DDM 885 Information"));

        setLayout(new MigLayout("insets 10", "[grow]10[grow,fill]"));

        tempLabel = new JLabel("Product Key");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        productKeyLabel= new JLabel("N/A");
        this.add(productKeyLabel, "wrap,growx");

        tempLabel = new JLabel("Order ID");
        tempLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        this.add(tempLabel, "growx");

        orderIdLabel = new JLabel("N/A");
        this.add(orderIdLabel, "wrap,growx");
    }
}
