package ly.secore.compute.DeviceManagementTool.Event;

import java.util.EventObject;
import ly.secore.compute.DeviceManagementTool.DataModel.ProductDescriptor;

public class UpdateProductDescriptors extends EventObject {
    private static final long serialVersionUID = 1L;
    private final ProductDescriptor[] productDescriptors;

    public UpdateProductDescriptors(Object source, ProductDescriptor[] productDescriptors) {
        super(source);
        this.productDescriptors = productDescriptors;
    }

    public ProductDescriptor[] getProductDescriptors() {
        return productDescriptors;
    }
}
