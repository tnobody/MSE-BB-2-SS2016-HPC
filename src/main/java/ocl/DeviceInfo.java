package ocl;

import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.clGetDeviceIDs;

/**
 * Created by tim on 19.05.2016.
 */
public class DeviceInfo {

    private long deviceType = CL_DEVICE_TYPE_ALL;
    private Platform platform;

    public DeviceInfo(Platform platform) {
        this.platform = platform;
    }

    public long getDeviceType() {
        return deviceType;
    }


    private OptionalInt numDevices = OptionalInt.empty();

    public int getNumDevices() {
        if(!numDevices.isPresent()) {
            int numDevicesArray[] = new int[1];
            clGetDeviceIDs(platform.getId(), deviceType, 0, null, numDevicesArray);
            numDevices = Arrays.stream(numDevicesArray).findFirst();
        }
        return numDevices.orElse(0);
    }

    public Platform getPlatform() {
        return platform;
    }

    List<cl_device_id> deviceIds = null;

    public List<cl_device_id> getDeviceIds() {
        if(deviceIds == null) {
            int deviceCount = getNumDevices();
            cl_device_id devices[] = new cl_device_id[deviceCount];
            clGetDeviceIDs(platform.getId(), deviceType, deviceCount, devices, null);
        }
        return deviceIds;
    }

}
