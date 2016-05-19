package org.tnobody.hpc;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.jocl.*;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.jocl.CL.*;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateKernel;

/**
 * Created by tim on 17.05.2016.
 */
public class OpenCLContext {

    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final cl_kernel kernel;

    public OpenCLContext(String programSource) throws InvalidArgumentException {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Create a context for the selected device
        context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Check if images are supported
        int imageSupport[] = new int[1];
        clGetDeviceInfo (device, CL.CL_DEVICE_IMAGE_SUPPORT,
                Sizeof.cl_int, Pointer.to(imageSupport), null);
        System.out.println("Images supported: "+(imageSupport[0]==1));
        if (imageSupport[0]==0)
        {
            throw new InvalidArgumentException(new String[]{"Images are not supported"});
        }

        // Create a command-queue
        System.out.println("Creating command queue...");
        long properties = 0;
        properties |= CL_QUEUE_PROFILING_ENABLE;
        //properties |= CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
        commandQueue = clCreateCommandQueue(context, device, properties, null);

        // Create the program
        System.out.println("Creating program...");
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        System.out.println("Building program...");
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        System.out.println("Creating kernel...");
        kernel = clCreateKernel(program, "rotateImage", null);
    }

    public cl_mem[] getImageMem(BufferedImage inputImage)
    {
        // Create the memory object for the input- and output image
        DataBufferInt dataBufferSrc =
                (DataBufferInt)inputImage.getRaster().getDataBuffer();
        int dataSrc[] = dataBufferSrc.getData();

        int imageSizeX = inputImage.getWidth();
        int imageSizeY = inputImage.getHeight();

        cl_image_format imageFormat = new cl_image_format();
        imageFormat.image_channel_order = CL_RGBA;
        imageFormat.image_channel_data_type = CL_UNSIGNED_INT8;

        return new cl_mem[]{
                clCreateImage2D(
                        context, CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR,
                        new cl_image_format[]{imageFormat}, imageSizeX, imageSizeY,
                        imageSizeX * Sizeof.cl_uint, Pointer.to(dataSrc), null)
                ,
                clCreateImage2D(
                        context, CL_MEM_WRITE_ONLY,
                        new cl_image_format[]{imageFormat}, imageSizeX, imageSizeY,
                        0, null, null)
        };
    }

    public cl_context getContext() {
        return context;
    }

    public cl_command_queue getCommandQueue() {
        return commandQueue;
    }

    public cl_kernel getKernel() {
        return kernel;
    }
}
