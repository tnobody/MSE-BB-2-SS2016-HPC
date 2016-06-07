package org.tnobody.hpc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Created by tim on 26.11.2015.
 */
public class JavaScan {

    public static <I, O> List<O> scan(BiFunction<O, I, O> fn, O init, List<I> in) {
        List<O> out = new ArrayList<>();
        _scan(fn, init, in, out);
        return out;
    }

    private static <I, O> void _scan(BiFunction<O, I, O> fn, O init, List<I> in, List<O> out) {
        O current = fn.apply(init,in.get(0));
        out.add(current);
        if (in.size() - 1 > 0) {
            _scan(fn, current, in.subList(1, in.size()), out);
        }
    }
}
