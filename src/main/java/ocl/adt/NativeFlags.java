package ocl.adt;

import java.util.Arrays;

/**
 * Created by tim on 24.05.2016.
 */
public interface NativeFlags {

    public long nativeValue();

    public static long combine(NativeFlags...types) {
        return Arrays.stream(types)
                .map(NativeFlags::nativeValue)
                .reduce(1L, (agg,nv) -> agg | nv);
    }
}
