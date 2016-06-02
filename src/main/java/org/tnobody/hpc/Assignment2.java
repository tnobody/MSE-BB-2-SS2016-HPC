package org.tnobody.hpc;

import static org.jocl.CL.*;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;


public class Assignment2 {

    // muss power of two sein
    private static final long N = 8;
    private static  String programSource;

    public static void main(String args[]) throws IOException {
        programSource = FileUtils.readLines(new File(Assignment2.class.getResource(".").getPath() + "/scan_kernel.c")).stream().collect(Collectors.joining(System.getProperty("line.separator")));
        Random rand = new Random();
        // Create input- and output data
        //long srcArrayA[] = new long[] {3,1,7,0,4,1,6,3};
        long srcArrayA[] = LongStream.range(0,N).map(l -> rand.nextLong()).toArray();
        long temp[] = new long[srcArrayA.length];
        Arrays.fill(temp,0);

        int n = srcArrayA.length;
        long dstArray[] = new long[n];

        Pointer srcA = Pointer.to(srcArrayA);
        Pointer dst = Pointer.to(dstArray);

        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 1;

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
        cl_context context = clCreateContext(
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);

        // Create a command-queue for the selected device
        cl_command_queue commandQueue =
                clCreateCommandQueue(context, device, 0, null);

        // Allocate the memory objects for the input- and output data
        cl_mem memObjects[] = new cl_mem[3];
        memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * n, srcA, null);
        memObjects[1] = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * n, dst, null);
        memObjects[2] = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * n * 2, Pointer.to(temp), null);

        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);

        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, "scan", null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[] { n }));
        clSetKernelArg(kernel, 3, Sizeof.cl_long * n * 2, null);

        // zusätzlicher output array, der das letzte element jedes scans speichert => pro workgroup wieder aufsummiert
        // dafür nochmal einen eigenen kernel

        // work size abfragen
        // JOCLDeviceQuery

        // Set the work-item dimensions
        long global_work_size[] = new long[1];
        global_work_size[0] = n;

        long local_work_size[] = new long[] {n/2};

        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);

        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[1], CL_TRUE, 0,
                n * Sizeof.cl_long, dst, 0, null, null);

        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);

        System.out.println("Result: "+java.util.Arrays.toString(dstArray));
    }
}
