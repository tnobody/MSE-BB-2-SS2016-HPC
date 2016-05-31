package ocl.platform;

import ocl.adt.ErrorFlags;
import ocl.adt.OpenClException;
import ocl.platform.device.Device;
import org.jocl.cl_command_queue;
import org.jocl.cl_queue_properties;

import static org.jocl.CL.clCreateCommandQueueWithProperties;

/**
 * Created by tim on 24.05.2016.
 */
public class CommandQueue {
    private cl_queue_properties clQueueProperties = new cl_queue_properties();
    private Context context;
    private Device device;

    public CommandQueue context(Context context) {
        this.context = context;
        return this;
    }

    public CommandQueue device(Device device) {
        this.device = device;
        return this;
    }

    public cl_command_queue toNative() throws OpenClException {
        int[] error = new int[1];
        cl_command_queue commandqueue = clCreateCommandQueueWithProperties(
                context.toNative(),
                device.getId(),
                clQueueProperties,
                error);
        if(error[0] != ErrorFlags.SUCCESS.nativeValue()) {
            throw new OpenClException(ErrorFlags.fromNative(error[0]));
        }
        return commandqueue;
    }
}
