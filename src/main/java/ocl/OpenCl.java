package ocl;

import org.jocl.CL;

/**
 * Created by tim on 19.05.2016.
 */
public class OpenCl {

    public void enableException() {
        CL.setExceptionsEnabled(true);
    }

    public void disableException() {
        CL.setExceptionsEnabled(false);
    }
}
