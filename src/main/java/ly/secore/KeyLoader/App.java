package ly.secore.KeyLoader;

import com.formdev.flatlaf.FlatLightLaf;
import java.io.FileInputStream;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import ly.secore.KeyLoader.GUI.IncarnationInformationPanel;
import ly.secore.KeyLoader.GUI.ManufacturingInformationPanel;
import ly.secore.KeyLoader.GUI.DDM885InformationPanel;
import ly.secore.KeyLoader.Database.ProductDescriptor;
import ly.secore.KeyLoader.Database.JsonReader;
import ly.secore.compute.ComputeDevice;
import net.miginfocom.swing.MigLayout;

/**
 * Main application class for displaying manufacturing and incarnation information.
 */

public class App {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java -jar KeyLoader.jar <path_to_device>");
      System.exit(1);
    }

    if (args.length > 1) {
      try (FileInputStream jsonInputStream = new FileInputStream(args[1])) {
        for (ProductDescriptor product : JsonReader.getProductDescriptors(jsonInputStream)) {
          System.out.println(product);
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to read JSON file: " + args[1], e);
      }
    }

    try (ComputeDevice computeDevice = new ComputeDevice(args[0]))
    {
        ComputeDevice.ManufacturingInfo mfgInfo;
        ComputeDevice.ReincarnationInfo incInfo;
        ComputeDevice.DDM885Info ddm885Info;

        FlatLightLaf.setup();

        // Create and display the manufacturing information panel
        ManufacturingInformationPanel manufacturingInfoPanel = new ManufacturingInformationPanel();
        IncarnationInformationPanel incarnationInfoPanel = new IncarnationInformationPanel();
        DDM885InformationPanel ddm885InfoPanel = new DDM885InformationPanel();
        JFrame frame = new JFrame("Manufacturing Information");

        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new MigLayout("wrap 1", "[grow]", "[]10[]"));
            frame.add(manufacturingInfoPanel, "growx");
            frame.add(incarnationInfoPanel, "growx");
            frame.add(ddm885InfoPanel, "growx");
            frame.pack();
            frame.setLocationRelativeTo(null);
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
            frame.setLocationRelativeTo(null);
        });
    }
    catch (Exception e)
    {
        e.printStackTrace(System.out);
    }
  }
}
