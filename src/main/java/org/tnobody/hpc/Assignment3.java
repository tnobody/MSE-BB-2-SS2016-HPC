package org.tnobody.hpc;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.jocl.CL.*;
import static org.jocl.CL.clCreateKernel;

/**
 * Created by tim on 11.06.2016.
 */
public class Assignment3 {

    Assignment2 scan = new Assignment2();

    final String KERNEL_METHOD = "scatter";

//    final String FILTER_PREDICATE_KERNEL_METHOD = "isEven";
//    final String FILTER_PREDICATE_KERNEL_METHOD = "isOdd";
    final String FILTER_PREDICATE_KERNEL_METHOD = "isLowerThen1000";

    final int PLATFORM_INDEX = 0;
    final long DEVICE_TYPE = CL_DEVICE_TYPE_GPU;
    final int DEVICE_INDEX = 0;
    private cl_mem inputBuffer;
    private cl_mem filterResultBuffer;
    private cl_mem scannedfilterResultsBuffer;
    private cl_mem outputBuffer;

    public Assignment3() throws IOException {
        String programSource = readProgramSource("/stream_compaction.c");

        // Create input- and output data
        final AtomicInteger c = new AtomicInteger();
        int input[] = Arrays.stream(new int[(int) Math.pow(2, 15)]).map((i) -> c.getAndIncrement()).toArray();

        int filterResults[] = new int[input.length];
        int scannedfilterResults[] = new int[input.length];
        int output[]; // is setted after we have a scannedFilterResult

        int workItemSize = 512;

        final long deviceType = CL_DEVICE_TYPE_ALL;

        CL.setExceptionsEnabled(true);

        int numPlatformArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformArray);

        cl_platform_id platform = getClPlatformId();
        cl_device_id device = getClDeviceId(platform);
        cl_context context = getClContext(platform, device);
        cl_command_queue commandQueue = getClCommandQueue(device, context);

        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);


        cl_program program = clCreateProgramWithSource(context, 1, new String[]{ programSource }, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel filterKernel = clCreateKernel(program, FILTER_PREDICATE_KERNEL_METHOD, null);

        inputBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * input.length, Pointer.to(input), null);
        filterResultBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * input.length, Pointer.to(filterResults), null);

        clSetKernelArg(filterKernel,0,Sizeof.cl_mem,Pointer.to(inputBuffer));
        clSetKernelArg(filterKernel,1,Sizeof.cl_mem,Pointer.to(filterResultBuffer));

        clEnqueueNDRangeKernel(
                commandQueue,
                filterKernel,
                1,
                null,
                new long[]{input.length},
                new long[]{workItemSize},
                0,
                null,
                null);

        clEnqueueReadBuffer(commandQueue, filterResultBuffer, CL_TRUE, 0, input.length * Sizeof.cl_int, Pointer.to(filterResults), 0, null, null);

        System.out.println("Input: " + Arrays.toString(input));
        //System.out.println("Filterresults: " + Arrays.toString(filterResults));

        scan.oclScan(workItemSize, filterResults, scannedfilterResults, new int[2]);

        //System.out.println("Scanned Filters: " + Arrays.toString(scannedfilterResults));

        cl_kernel scatterKernel = clCreateKernel(program, KERNEL_METHOD, null);

        output = new int[scannedfilterResults[scannedfilterResults.length-1]];
        scannedfilterResultsBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * scannedfilterResults.length, Pointer.to(scannedfilterResults), null);
        outputBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * output.length, Pointer.to(output), null);

        clSetKernelArg(scatterKernel,0,Sizeof.cl_mem,Pointer.to(inputBuffer));
        clSetKernelArg(scatterKernel,1,Sizeof.cl_mem,Pointer.to(filterResultBuffer));
        clSetKernelArg(scatterKernel,2,Sizeof.cl_mem,Pointer.to(scannedfilterResultsBuffer));
        clSetKernelArg(scatterKernel,3,Sizeof.cl_mem,Pointer.to(outputBuffer));

        clEnqueueNDRangeKernel(
                commandQueue,
                scatterKernel,
                1,
                null,
                new long[]{input.length},
                new long[]{workItemSize},
                0,
                null,
                null);

        clEnqueueReadBuffer(commandQueue, outputBuffer, CL_TRUE, 0, output.length * Sizeof.cl_int, Pointer.to(output), 0, null, null);
        System.out.println("Result: " + Arrays.toString(output));
    }

    private String readProgramSource(String file) {
        try {
            return FileUtils.readLines(new File(Assignment2.class.getResource(".").getPath() + file)).stream().collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }



    private cl_command_queue getClCommandQueue(cl_device_id device, cl_context context) {
        // Create a command-queue for the selected device
        return clCreateCommandQueue(context, device, 0, null);
    }

    private cl_context getClContext(cl_platform_id platform, cl_device_id device) {
        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        // Create a context for the selected device
        return clCreateContext(
                contextProperties,
                1,
                new cl_device_id[]{ device },
                null,
                null,
                null);
    }

    private cl_device_id getClDeviceId(cl_platform_id platform) {
        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, DEVICE_TYPE, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, DEVICE_TYPE, numDevices, devices, null);
        return devices[DEVICE_INDEX];
    }

    private cl_platform_id getClPlatformId() {
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        return platforms[PLATFORM_INDEX];
    }

    public static void main(String[] args) {
        try {
            new Assignment3();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
