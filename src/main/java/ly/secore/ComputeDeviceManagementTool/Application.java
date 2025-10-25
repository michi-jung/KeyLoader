package ly.secore.ComputeDeviceManagementTool;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.FileInputStream;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.JFrame;

import ly.secore.ComputeDeviceManagementTool.DataModel.JsonReader;
import ly.secore.ComputeDeviceManagementTool.DataModel.ProductDescriptor;
import ly.secore.ComputeDeviceManagementTool.GUI.DDM885InformationPanel;
import ly.secore.ComputeDeviceManagementTool.GUI.IncarnationInformationPanel;
import ly.secore.ComputeDeviceManagementTool.GUI.ManufacturingInformationPanel;
import ly.secore.ComputeDeviceManagementTool.GUI.ProductDescriptorPanel;
import ly.secore.compute.ComputeDevice;
import net.miginfocom.swing.MigLayout;

/**
 * Main application class for displaying manufacturing and incarnation information.
 */

public class Application {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java -jar KeyLoader.jar <path_to_device>");
      System.exit(1);
    }

    try (ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
        ComputeDevice.ManufacturingInfo mfgInfo;
        ComputeDevice.ReincarnationInfo incInfo;
        ComputeDevice.DDM885Info ddm885Info;
        final ProductDescriptor productDescriptor;

        try (FileInputStream jsonInputStream = new FileInputStream(args[1])) {
            List<ProductDescriptor> productDescriptors = JsonReader.getProductDescriptors(jsonInputStream);

            if (productDescriptors.isEmpty()) {
                throw new RuntimeException("No product descriptors found in JSON file: " + args[1]);
            }

            productDescriptor = productDescriptors.get(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON file: " + args[1], e);
        }

        FlatLightLaf.setup();

        // Create and display the manufacturing information panel
        ManufacturingInformationPanel manufacturingInfoPanel = new ManufacturingInformationPanel();
        IncarnationInformationPanel incarnationInfoPanel = new IncarnationInformationPanel();
        DDM885InformationPanel ddm885InfoPanel = new DDM885InformationPanel();
        ProductDescriptorPanel productDescriptorPanel = new ProductDescriptorPanel();

        JFrame frame = new JFrame("compute secore.ly Device Management Tool");

         SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new MigLayout("wrap 1", "[grow]", "[]10[]"));
            frame.add(manufacturingInfoPanel, "growx");
            frame.add(incarnationInfoPanel, "growx");
            frame.add(ddm885InfoPanel, "growx");
            frame.add(productDescriptorPanel, "growx");
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setVisible(true);
        });

        computeDevice.openServiceSession();

        mfgInfo = computeDevice.getManufacturingInfo();
        incInfo = computeDevice.getReincarnationInfo();
        ddm885Info = computeDevice.getDDM885Info();

        computeDevice.closeServiceSession();

        SwingUtilities.invokeLater(() -> {
            manufacturingInfoPanel.setManufacturingInfo(mfgInfo);
            incarnationInfoPanel.setIncarnationInfo(incInfo);
            ddm885InfoPanel.setIncarnationInfo(ddm885Info);
            productDescriptorPanel.setProductDescriptor(productDescriptor);
            frame.setLocationRelativeTo(null);
        });
    }
    catch (Exception e)
    {
        e.printStackTrace(System.out);
    }
  }
}
