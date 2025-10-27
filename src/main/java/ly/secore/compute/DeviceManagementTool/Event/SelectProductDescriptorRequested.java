package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventObject;
import ly.secore.compute.DeviceManagementTool.DataModel.ProductDescriptor;

public class SelectProductDescriptorRequested extends EventObject {
    private static final long serialVersionUID = 1L;
    private final ProductDescriptor productDescriptor;

    public SelectProductDescriptorRequested(Object source, ProductDescriptor productDescriptor) {
        super(source);
        this.productDescriptor = productDescriptor;
    }

    public ProductDescriptor getProductDescriptor() {
        return productDescriptor;
    }
}
