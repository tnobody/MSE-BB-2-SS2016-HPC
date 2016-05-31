package org.tnobody.hpc;

import ocl.OpenCl;
import ocl.adt.OpenClException;
import ocl.platform.Context;
import ocl.platform.device.DeviceFlags;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;

import java.util.Optional;

import static org.jocl.CL.CL_CONTEXT_DEVICES;
import static org.jocl.CL.CL_DEVICE_TYPE_GPU;
import static org.jocl.CL.clCreateContextFromType;

/**
 * Created by tim on 25.05.2016.
 */
public class App {

    public static void main(String[] args) {
        OpenCl ocl = new OpenCl();
        ocl.enableException();
        ocl
                .getPlatformInfo()
                .getPlatforms()
                .findFirst()
                .map(p -> p.getDeviceInfo())
                .map(di -> di.flag(DeviceFlags.GPU).getDevices().findFirst().get())
                .map(d -> d.createContext())
                .ifPresent(c -> {
                    try {
                        c.createCommandQueue();
                        c.createBuffer().
                    } catch (OpenClException e) {
                        e.printStackTrace();
                    }
                });
        /*
        int[] ciErrNum = new int[1];
        cl_context_properties clContextProperties = new cl_context_properties();
        CL_CONTEXT_DEVICES
        cl_context myctx = clCreateContextFromType (
                clContextProperties, CL_DEVICE_TYPE_GPU,
                null, null, ciErrNum);
        System.out.println("Hello");
        */
    }

}
