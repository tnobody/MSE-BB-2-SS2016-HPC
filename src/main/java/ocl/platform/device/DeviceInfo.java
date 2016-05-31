package ocl.platform.device;

import ocl.adt.NativeFlags;
import ocl.platform.Context;
import ocl.platform.Platform;
import org.jocl.cl_device_id;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

import static org.jocl.CL.clGetDeviceIDs;

/**
 * Created by tim on 19.05.2016.
 */
public class DeviceInfo {

    private List<DeviceFlags> deviceFlags = Arrays.asList(DeviceFlags.DEFAULT);
    private Platform platform;

    public DeviceInfo(Platform platform) {
        this.platform = platform;
    }

    public DeviceInfo flag(DeviceFlags flag) {
        deviceFlags.add(flag);
        return this;
    }

    private OptionalInt numDevices = OptionalInt.empty();

    public int getNumDevices() {
        if(!numDevices.isPresent()) {
            int numDevicesArray[] = new int[1];
            clGetDeviceIDs(
                    platform.getId(),
                    NativeFlags.combine(deviceFlags.toArray(new DeviceFlags[deviceFlags.size()])),
                    0,
                    null,
                    numDevicesArray);
            numDevices = Arrays.stream(numDevicesArray).findFirst();
        }
        return numDevices.orElse(0);
    }

    public Platform getPlatform() {
        return platform;
    }

    Stream<Device> deviceIds = null;

    public Stream<Device> getDevices() {
        if(deviceIds == null) {
            int deviceCount = getNumDevices();
            cl_device_id devices[] = new cl_device_id[deviceCount];
            clGetDeviceIDs(
                    platform.getId(),
                    NativeFlags.combine(deviceFlags.toArray(new DeviceFlags[deviceFlags.size()])),
                    deviceCount,
                    devices,
                    null);
            deviceIds = Arrays.stream(devices).map(Device::new);
        }
        return deviceIds;
    }

    public Context createContext() {
        return (new Context()).platform(platform).deviceInfo(this);
    }

}
