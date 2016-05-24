package ocl;

import org.jocl.CL;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.jocl.CL.*;

/**
 * Created by tim on 19.05.2016.
 */
public class OpenCl {

    private PlatformInfo platformInfo = new PlatformInfo();

    public void enableException() {
        CL.setExceptionsEnabled(true);
    }

    public void disableException() {
        CL.setExceptionsEnabled(false);
    }

    public OpenClContext createContextFor(Platform platform) {
        OpenClContext openClContext = new OpenClContext();
        openClContext.setPlatform(platform);
        return openClContext;
    }
}
