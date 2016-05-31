package ocl.platform.device;

import ocl.platform.Context;
import org.jocl.cl_device_id;

/**
 * Created by tim on 24.05.2016.
 */
public class Device {

    private cl_device_id id;

    public Device(cl_device_id id) {
        this.id = id;
    }

    public cl_device_id getId() {
        return id;
    }

    public Context createContext() {
        return new Context().
    }
}
