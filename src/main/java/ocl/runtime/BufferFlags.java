package ocl.runtime;

import ocl.adt.NativeFlags;

import java.util.Arrays;

import static org.jocl.CL.*;

/**
 * Created by tim on 24.05.2016.
 */
public enum BufferFlags implements NativeFlags {
    READ_WRITE(CL_MEM_READ_WRITE),

    WRITE_ONLY(CL_MEM_WRITE_ONLY),

    READ_ONLY(CL_MEM_READ_ONLY),

    USE_HOST_PTR(CL_MEM_USE_HOST_PTR),

    ALLOC_HOST_PTR(CL_MEM_ALLOC_HOST_PTR),

    COPY_HOST_PTR(CL_MEM_COPY_HOST_PTR),

    // OPENCL_1_2
    HOST_WRITE_ONLY(CL_MEM_HOST_WRITE_ONLY),

    HOST_READ_ONLY(CL_MEM_HOST_READ_ONLY),

    HOST_NO_ACCESS(CL_MEM_HOST_NO_ACCESS),

    // OPENCL_2_0
    SVM_FINE_GRAIN_BUFFER(CL_MEM_SVM_FINE_GRAIN_BUFFER),

    SVM_ATOMICS(CL_MEM_SVM_ATOMICS);

    private long value;

    BufferFlags(long value) {
        this.value = value;
    }

    public long nativeValue() {
        return value;
    }

    public static long combine(BufferFlags...types) {
        return Arrays.stream(types)
                .map(BufferFlags::nativeValue)
                .reduce(1L, (agg,nv) -> agg | nv);
    }
}
