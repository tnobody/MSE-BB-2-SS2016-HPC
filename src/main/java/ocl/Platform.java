package ocl;

import org.jocl.cl_platform_id;

/**
 * Created by tim on 19.05.2016.
 */
public class Platform {
    private cl_platform_id id;
    private DeviceInfo deviceInfo;

    public Platform(cl_platform_id id) {
        this.id = id;
    }

    public cl_platform_id getId() {
        return id;
    }

    public DeviceInfo getDeviceInfo() {
        if(deviceInfo == null) {
            deviceInfo = new DeviceInfo(this);
        } return deviceInfo;
    }
}
