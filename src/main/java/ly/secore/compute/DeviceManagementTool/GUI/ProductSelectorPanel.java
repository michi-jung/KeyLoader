package ly.secore.compute.DeviceManagementTool.GUI;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.DataModel.ProductDescriptor;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.Listener;
import ly.secore.compute.DeviceManagementTool.Event.UpdateDeviceInformationRequested;
import ly.secore.compute.DeviceManagementTool.Event.UpdateProductDescriptors;

public class ProductSelectorPanel extends JPanel implements ActionListener, Listener {

    private static final long serialVersionUID = 1L;
    private final EventBus eventBus;
    private JComboBox<String> productDescriptorsComboBox;
    private ProductDescriptor[] productDescriptors;
    private ProductDescriptorPanel productDescriptorPanel;

    public ProductSelectorPanel(EventBus eventBus) {
        this.eventBus = eventBus;
        eventBus.addListener(this);
        initComponents();
    }

    public ProductDescriptor getSelectedProductDescriptor() {
        int selectedIndex = productDescriptorsComboBox.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < productDescriptors.length) {
            return productDescriptors[selectedIndex];
        }
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        productDescriptorsComboBox.setEnabled(enabled);
        productDescriptorPanel.setEnabled(enabled);
    }

    public void actionRequested(EventObject event) {
        if (event instanceof UpdateProductDescriptors) {
            UpdateProductDescriptors updateEvent = (UpdateProductDescriptors)event;

            productDescriptors = updateEvent.getProductDescriptors();

            productDescriptorsComboBox.removeAllItems();
            for (ProductDescriptor descriptor : productDescriptors) {
                productDescriptorsComboBox.addItem(descriptor.getProductKey() + " - " +
                    descriptor.getDeviceTypeDescriptor().getDescription());
            }
            productDescriptorsComboBox.addItem(new String("None"));
            productDescriptorsComboBox.setSelectedIndex(productDescriptors.length);

            SwingUtilities.windowForComponent(this).pack();
        } else if (event instanceof UpdateDeviceInformationRequested) {
            Device.DDM885Info ddm885Info = ((UpdateDeviceInformationRequested)event)
                .getDeviceInformation().getDDM885Info();

            if (ddm885Info == null || ddm885Info.productKey == null) {
                productDescriptorsComboBox.setSelectedIndex(productDescriptors.length);
            } else {
                for (int i = 0; i < productDescriptors.length; i++) {
                    if (productDescriptors[i].getProductKey().equals(ddm885Info.productKey)) {
                        productDescriptorsComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == productDescriptorsComboBox) {
            eventBus.selectProductDescriptor(getSelectedProductDescriptor());
        }
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();

        productDescriptorsComboBox = new JComboBox<>();
        productDescriptorsComboBox.addActionListener(this);
        productDescriptorPanel = new ProductDescriptorPanel(eventBus);

        setLayout(new GridBagLayout());

        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;   // expand horizontally
        gbc.weighty = 0.0;   // no vertical expansion
        gbc.insets = new Insets(5, 0, 5, 0);

        add(productDescriptorsComboBox, gbc);

        gbc.weighty = 1.0; // allow vertical expansion

        add(productDescriptorPanel, gbc);
    }
}
