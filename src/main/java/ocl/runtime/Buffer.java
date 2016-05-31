package ocl.runtime;

import ocl.adt.ErrorFlags;
import ocl.adt.NativeFlags;
import ocl.adt.OpenClException;
import ocl.platform.Context;
import org.jocl.Pointer;
import org.jocl.cl_mem;

import java.util.ArrayList;
import java.util.List;

import static org.jocl.CL.clCreateBuffer;

/**
 * Created by tim on 24.05.2016.
 */
public class Buffer {

    private Context context;
    private List<BufferFlags> flags = new ArrayList<>();
    private int size = 0;
    private Pointer pointer = null;

    public Buffer flag(BufferFlags bufferFlags) {
        flags.add(bufferFlags);
        return this;
    }

    public Buffer context(Context context) {
        this.context = context;
        return this;
    }

    public Buffer size(int s) {
        size = s;
        return this;
    }

    public Buffer pointer(Pointer pointer) {
        this.pointer = pointer;
        return this;
    }

    public cl_mem toNative() throws OpenClException {
        int[] errors = new int[1];
        cl_mem mem = clCreateBuffer(
                context.getNativeContext(),
                NativeFlags.combine(flags.toArray(new BufferFlags[flags.size()])),
                size,
                pointer,
                errors
        );
        if(errors[0] != ErrorFlags.SUCCESS.nativeValue()) {
            throw new OpenClException(ErrorFlags.fromNative(errors[0]));
        }
        return mem;
    }
}
