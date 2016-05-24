package ocl;

import org.jocl.cl_context_properties;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;

/**
 * Created by tim on 19.05.2016.
 */
public class OpenClContext {
    private Platform platform;
    private cl_context_properties clContextProperties = new cl_context_properties();

    public void setPlatform(Platform platform) {
        clContextProperties.addProperty(CL_CONTEXT_PLATFORM,platform.getId());
    }
}
