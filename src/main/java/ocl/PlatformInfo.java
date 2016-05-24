package ocl;

import org.jocl.cl_platform_id;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.jocl.CL.clGetPlatformIDs;

/**
 * Created by tim on 19.05.2016.
 */
public class PlatformInfo {

    private List<cl_platform_id> platformIds = null;

    public List<cl_platform_id> getPlatformIds() {
        if(platformIds == null) {
            int platformCount = getNumPlatforms();
            cl_platform_id[] platformIdsBag = new cl_platform_id[platformCount];
            clGetPlatformIDs(platformCount, platformIdsBag, null);
            platformIds = Arrays
                    .stream(platformIdsBag)
                    .collect(Collectors.toList());
        }
        return platformIds;
    }

    private OptionalInt numPlatForms = OptionalInt.empty();

    public int getNumPlatforms() {
        if (!numPlatForms.isPresent()) {
            int numPlatformsArray[] = new int[1];
            clGetPlatformIDs(0, null, numPlatformsArray);
            numPlatForms = Arrays
                    .stream(numPlatformsArray)
                    .findFirst();
        }
        return numPlatForms.orElse(0);
    }

}
