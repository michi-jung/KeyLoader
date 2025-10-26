package ly.secore.compute.DeviceManagementTool.GUI;

import java.net.URL;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.batik.swing.JSVGCanvas;

public class LifecycleInformationPanel extends JPanel {
    private JSVGCanvas svgCanvas;

    public LifecycleInformationPanel() {
        URL deviceLifeCycleSvgUrl = getClass().getResource("device-life-cycle.svg");

        setLayout(new MigLayout("fill, insets 0"));
        svgCanvas = new JSVGCanvas();
        svgCanvas.setURI(deviceLifeCycleSvgUrl.toExternalForm());
        add(svgCanvas, "grow, push");
    }
}
