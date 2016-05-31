package ocl.platform;

import ocl.adt.ErrorFlags;
import ocl.adt.OpenClException;
import ocl.platform.device.Device;
import ocl.platform.device.DeviceInfo;
import ocl.runtime.Buffer;
import org.jocl.*;

import static org.jocl.CL.*;

/**
 * Created by tim on 19.05.2016.
 */
public class Context {

    private cl_context_properties contextProperties = new cl_context_properties();
    private DeviceInfo deviceInfo;
    private Device device;

    public cl_context toNative() throws OpenClException {
        int[] error = new int[1];
        cl_context nativeContext = clCreateContext(
                contextProperties,
                0,
                deviceInfo.getDevices().map(d -> d.getId()).toArray(s -> new cl_device_id[s]),
                null,
                null,
                error);
        if(error[0] != ErrorFlags.SUCCESS.nativeValue()) {
            throw new OpenClException(ErrorFlags.fromNative(error[0]));
        }
        return nativeContext;
    }

    public Context platform(Platform platform) {
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform.getId());
        return this;
    }


    public Context deviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    public Context device(Device device) {
        this.device = device;
    }

    public CommandQueue createCommandQueue() throws OpenClException {
        return (new CommandQueue()).context(this);
    }


    public Buffer createBuffer() {
        return (new Buffer()).context(this);
    }
}
