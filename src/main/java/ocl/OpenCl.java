package ocl;

import ocl.platform.PlatformInfo;
import org.jocl.CL;

/**
 * Created by tim on 19.05.2016.
 */
public class OpenCl {

    private PlatformInfo platformInfo = new PlatformInfo();

    public OpenCl enableException() {
        CL.setExceptionsEnabled(true);
        return this;
    }

    public OpenCl disableException() {
        CL.setExceptionsEnabled(false);
        return this;
    }

    public PlatformInfo getPlatformInfo() {
        return platformInfo;
    }

}
