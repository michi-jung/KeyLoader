package ly.secore.ComputeDeviceManagementTool.GUI;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import ly.secore.ComputeDeviceManagementTool.DataModel.ApplicationDescriptor;
import ly.secore.ComputeDeviceManagementTool.DataModel.DevicePersonalityDescriptor;
import ly.secore.ComputeDeviceManagementTool.DataModel.DeviceTypeDescriptor;
import ly.secore.ComputeDeviceManagementTool.DataModel.FirmwareDescriptor;
import ly.secore.ComputeDeviceManagementTool.DataModel.ProductDescriptor;
import net.miginfocom.swing.MigLayout;

public class ProductDescriptorPanel extends JPanel {
    private static class TreeModelBuilder {
    
        public static DefaultMutableTreeNode build(String label, FirmwareDescriptor firmwareDescriptor) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(label);
        
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Version</b> %s</html>", firmwareDescriptor.getVersion()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>File Name of Firmware Image</b> %s</html>", firmwareDescriptor.getFileName()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>File Name of Initial Firmware Image</b> %s</html>", firmwareDescriptor.getInitialFileName()), false));

            return root;
        }

        public static DefaultMutableTreeNode build(String label, ApplicationDescriptor applicationDescriptor) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(label);
        
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Description</b> %s</html>", applicationDescriptor.getDescription()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Application Class</b> %s</html>", applicationDescriptor.getApplicationClass()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Version</b> %s</html>", applicationDescriptor.getVersion()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>File Name</b> %s</html>", applicationDescriptor.getFileName()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>App Key Label</b> %s</html>", applicationDescriptor.getAppKeyLabel()), false));

            return root;
        }

        public static DefaultMutableTreeNode build(String label, DeviceTypeDescriptor deviceTypeDescriptor) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(label);
            
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Type</b> %s</html>", deviceTypeDescriptor.getType()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Description</b> %s</html>", deviceTypeDescriptor.getDescription()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Device Class</b> %s</html>", deviceTypeDescriptor.getDeviceClass()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Device Type ID</b> %d</html>", deviceTypeDescriptor.getDeviceTypeId()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>ECL</b> %d</html>", deviceTypeDescriptor.getEcl()), false));
            root.add(TreeModelBuilder.build("<html><b>Firmware</b></html>", deviceTypeDescriptor.getFirmwareDescriptor()));

            return root;
        }

        public static DefaultMutableTreeNode build(String label, DevicePersonalityDescriptor devicePersonalityDescriptor) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(label);
            
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Personality</b> %s</html>", devicePersonalityDescriptor.getPersonality()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Description</b> %s</html>", devicePersonalityDescriptor.getDescription()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Operating Mode</b> %d</html>", devicePersonalityDescriptor.getOperatingMode()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Variant</b> %d</html>", devicePersonalityDescriptor.getVariant()), false));
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Master Key</b> %s</html>", devicePersonalityDescriptor.getMasterKey()), false));
            root.add(TreeModelBuilder.build("<html><b>Firmware Descriptor</b></html>", devicePersonalityDescriptor.getFirmwareDescriptor()));
            root.add(TreeModelBuilder.build("<html><b>Application Descriptor</b></html>", devicePersonalityDescriptor.getApplication()));

            return root;
        }

        public static DefaultMutableTreeNode build(String label, ProductDescriptor productDescriptor) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(label);
            
            root.add(new DefaultMutableTreeNode(String.format("<html><b>Product Key</b> %s</html>", productDescriptor.getProductKey()), false));
            root.add(build("<html><b>Device Type Descriptor</b></html>", productDescriptor.getDeviceTypeDescriptor()));
            root.add(build("<html><b>Device Personality Descriptor</b></html>", productDescriptor.getDevicePersonalityDescriptor()));

            return root;
        }
    }
    
    private JTree productDescriptorTree;

    public ProductDescriptorPanel() {
        initComponents();
    }
    
    public void setProductDescriptor(ProductDescriptor productDescriptor) {
        DefaultTreeModel model = (DefaultTreeModel)productDescriptorTree.getModel();
        model.setRoot(TreeModelBuilder.build("Product Descriptor", productDescriptor));
        SwingUtilities.windowForComponent(this).pack();
    }

    public void initComponents() {
        setLayout(new MigLayout("insets 10", "[grow]"));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Product Configuration Tree")); 

        productDescriptorTree = new JTree(new DefaultTreeModel(null));
        productDescriptorTree.setRootVisible(false);
        productDescriptorTree.setShowsRootHandles(true);

        add(new JScrollPane(productDescriptorTree), "grow, push");
    }
}
