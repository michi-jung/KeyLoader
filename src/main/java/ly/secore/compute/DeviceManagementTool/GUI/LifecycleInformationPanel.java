package ly.secore.compute.DeviceManagementTool.GUI;

import java.util.EventObject;

import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;
import ly.secore.compute.Device;
import ly.secore.compute.DeviceManagementTool.Event.EventBus;
import ly.secore.compute.DeviceManagementTool.Event.Listener;
import ly.secore.compute.DeviceManagementTool.Event.UpdateDeviceInformationRequested;
import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class LifecycleInformationPanel extends JPanel implements Listener {
    private static final long serialVersionUID = 1L;
    private static final String SVG_FILE = "device-life-cycle.svg";
    private JSVGCanvas svgCanvas;
    private int lifecycleState = Device.LifecycleInfo.LIFECYCLE_STATE_UNKNOWN;

    public LifecycleInformationPanel(EventBus eventBus) {
        initComponents();
        eventBus.addListener(this);
    }

    private Element getLifecycleStateGroup(int lifecycleState) {
        if (lifecycleState < Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURED ||
            lifecycleState > Device.LifecycleInfo.LIFECYCLE_STATE_DECOMMISSIONED)
        {
            return null; // Invalid state
        }

        String stateName = Device.LifecycleInfo.getStateName(lifecycleState);
        return svgCanvas.getSVGDocument().getElementById("State_" + stateName.replace(" ", "_"));
    }

    protected void setStateFillColor(Element lifecycleStateGroup, String color) {
        NodeList elems;

        if (lifecycleStateGroup == null) {
            return; // No group for this state
        }

        elems = lifecycleStateGroup.getElementsByTagName("rect");

        if (elems.getLength() > 0) {
            Element rect = (Element)elems.item(0);
            String style = rect.getAttribute("style");
            style = style.replaceAll("fill:[^;]*", "fill:" + color);
            rect.setAttributeNS(null, "style", style);
        }

        elems = lifecycleStateGroup.getElementsByTagName("circle");

        if (elems.getLength() > 0) {
            Element circle = (Element)elems.item(0);
            String style = circle.getAttribute("style");
            style = style.replaceAll("fill:[^;]*", "fill:" + color);
            circle.setAttributeNS(null, "style", style);
        }
    }

    public void setLifecycleInformation(Device.LifecycleInfo lifecycleInfo) {
        if (lifecycleInfo != null) {
            svgCanvas.setVisible(true);

            if (lifecycleState == lifecycleInfo.state) {
                return; // No change
            }

            svgCanvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(() -> {

                /* First re-set fill attribute to inactive color on previous state */

                setStateFillColor(
                    getLifecycleStateGroup(lifecycleState),
                    lifecycleState == Device.LifecycleInfo.LIFECYCLE_STATE_MANUFACTURED
                        ? "#000000"   // Black for manufactured state
                        : "#ffffff"); // White for other states

                /* Then set new states color to gray */

                lifecycleState = lifecycleInfo.state;
                setStateFillColor(getLifecycleStateGroup(lifecycleState), "#b0b0b0");
            });
        } else {
            svgCanvas.setVisible(false);
        }
    }

    public void actionRequested(EventObject event) {
        if (event instanceof UpdateDeviceInformationRequested) {
            setLifecycleInformation(
                ((UpdateDeviceInformationRequested)event).getDeviceInformation()
                    .getLifecycleInfo());
        }
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 0"));
        svgCanvas = new JSVGCanvas();
        svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        svgCanvas.setURI(getClass().getResource(SVG_FILE).toExternalForm());
        add(svgCanvas, "grow, push");
    }
}