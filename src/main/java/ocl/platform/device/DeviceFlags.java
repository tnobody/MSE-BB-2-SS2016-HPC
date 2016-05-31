package ocl.platform.device;

import ocl.adt.NativeFlags;

import java.util.Arrays;

import static org.jocl.CL.*;

/**
 * Created by tim on 24.05.2016.
 */
public enum DeviceFlags implements NativeFlags {
    /*

    public static final long CL_DEVICE_TYPE_DEFAULT = (1 << 0);
    public static final long CL_DEVICE_TYPE_CPU = (1 << 1);
    public static final long CL_DEVICE_TYPE_GPU = (1 << 2);
    public static final long CL_DEVICE_TYPE_ACCELERATOR = (1 << 3);
    public static final long CL_DEVICE_TYPE_ALL = 0xFFFFFFFF;
    // OPENCL_1_2
    public static final long CL_DEVICE_TYPE_CUSTOM = (1 << 4);
     */

    DEFAULT(CL_DEVICE_TYPE_DEFAULT),

    CPU(CL_DEVICE_TYPE_CPU),

    GPU(CL_DEVICE_TYPE_GPU),

    ACCELERATOR(CL_DEVICE_TYPE_ACCELERATOR),

    ALL(CL_DEVICE_TYPE_ALL);


    private long value;

    DeviceFlags(long value) {
        this.value = value;
    }

    public long nativeValue() {
        return value;
    }

    public static long combine(DeviceFlags...types) {
        return Arrays.stream(types)
                .map(DeviceFlags::nativeValue)
                .reduce(1L, (agg,nv) -> agg | nv);
    }
}
