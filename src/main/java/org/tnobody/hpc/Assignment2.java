package org.tnobody.hpc;

import static org.jocl.CL.*;

import org.apache.commons.io.FileUtils;
import org.jocl.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;


public class Assignment2 {

//    // OpenCl Config
    private static String programSource;
    final int PLATFORM_INDEX = 0;
    final long DEVICE_TYPE = CL_DEVICE_TYPE_GPU;
    final int DEVICE_INDEX = 0;

    // Kernel
    final String KERNEL_FILE = "/Scan.c";
    final String SCAN_METHOD = "scan_work_efficient";
    final String ADD_METHOD = "add";

    private cl_mem inputBuffer;
    private cl_mem outputBuffer;
    private cl_mem sumBuffer;
    private cl_mem scanSumBuffer;


    private Assignment2() throws IOException {

        // Read the programSource from a file
        programSource = FileUtils.readLines(new File(Assignment2.class.getResource(".").getPath() + KERNEL_FILE)).stream().collect(Collectors.joining(System.getProperty("line.separator")));

        // Create input- and output data
        int input[] = new int[(int) Math.pow(2, 15)];
        Arrays.fill(input, 1);

        int output[] = new int[input.length];
        int sum[] = new int[2];


        int workItemSize = 512;



        // Time - Java
        long startJava = System.nanoTime();
        this.javaScan(input);
        System.out.println("Java took: " + (System.nanoTime() - startJava));

        // Time - OCL
        long startOcl = System.nanoTime();
        this.oclScan(workItemSize, input, output, sum);
        System.out.println("Ocl took: " + (System.nanoTime() - startOcl));
    }

    private void javaScan(int[] input) {
        //List<Long> out = JavaScan.scan((a,b) -> a+b, 0L, Arrays.stream(input).boxed().collect(Collectors.toList()));
        //System.out.println("With Java" + out);
    }

    private void oclScan(int workItemSize, int[]input, int[]output, int[]sum) {

        int globalWorkSize = input.length;
        int workGroupCount = (globalWorkSize + workItemSize - 1) / workItemSize;

        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        CL.setExceptionsEnabled(true);

        int numPlatformArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformArray);

        int numPlatforms = numPlatformArray[0];



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
        cl_kernel scanKernel = clCreateKernel(program, SCAN_METHOD, null);
        cl_kernel addKernel = clCreateKernel(program, ADD_METHOD, null);

        // Set the arguments for the kernel
        // Allocate the memory objects for the input- and output data
        getClMemoryBuffers(context, input, output, sum, workGroupCount);


//        startTime = System.nanoTime();

        executeScanKernel(commandQueue, scanKernel, globalWorkSize, workItemSize, inputBuffer, outputBuffer, sumBuffer);
//        System.out.println("AfterScanKernel 1 - StartTime " + startTime);
//        debug("After first", commandQueue, outputBuffer, src, dst);

        if (workGroupCount > 1) {

            executeScanKernel(commandQueue, scanKernel, workGroupCount, workItemSize, sumBuffer, sumBuffer, scanSumBuffer);
//            debug("After second ", commandQueue, outputBuffer, src, dst);
            System.out.println(commandQueue);

            if (workGroupCount > workItemSize) {
                executeScanKernel(commandQueue, scanKernel, workGroupCount, workItemSize, scanSumBuffer, scanSumBuffer, null);
//                debug("After third ", commandQueue, outputBuffer, src, dst);
                System.out.println(commandQueue);

                executeAddKernel(commandQueue, addKernel, globalWorkSize, workItemSize, scanSumBuffer, sumBuffer);
            }

            executeAddKernel(commandQueue, addKernel, globalWorkSize, workItemSize, sumBuffer, outputBuffer);
//            debug("After add  ", commandQueue);
        }

        debug("After all", commandQueue, outputBuffer, input, output);

        clEnqueueReadBuffer(commandQueue, outputBuffer, CL_TRUE, 0, input.length * Sizeof.cl_int, Pointer.to(output), 0, null, null);
        clean(context, commandQueue, program, scanKernel);
    }

    private void debug(String text, cl_command_queue commandQueue, cl_mem outputBuffer, int[] src, int[] dst) {
        clEnqueueReadBuffer(commandQueue, outputBuffer, CL_TRUE, 0, src.length * Sizeof.cl_int, Pointer.to(dst), 0, null, null);
        System.out.println("Input: " + text + Arrays.toString(src));
        System.out.println("Output: " + text + Arrays.toString(dst));
    }

    private static void executeScanKernel(cl_command_queue commandQueue, cl_kernel kernel, int workGroupCount, int workItemSize, cl_mem inputBuffer, cl_mem outputBuffer, cl_mem sumBuffer) {

        int localGroupCount = workGroupCount / 2;
        int localWorkItemSize = workItemSize > workGroupCount ? workGroupCount / 2 : workItemSize / 2;

        cl_mem tempMemObjects[] = new cl_mem[workGroupCount + 1];

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputBuffer));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{(int) workItemSize}));
        clSetKernelArg(kernel, 3, localWorkItemSize * 2 * Sizeof.cl_int, null);

        if (sumBuffer == null) {
            clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(new cl_mem[workGroupCount + 1]));
        } else {
            clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(sumBuffer));
        }


        // zusätzlicher output array, der das letzte element jedes scans speichert => pro workgroup wieder aufsummiert
        // dafür nochmal einen eigenen kernel

        clEnqueueNDRangeKernel(
                commandQueue,
                kernel,
                1,
                null,
                new long[]{localGroupCount},
                new long[]{localWorkItemSize},
                0,
                null,
                null);
    }


    private static void executeAddKernel(cl_command_queue commandQueue, cl_kernel kernel, int globalWorkSize, int workItemSize, cl_mem inputBuffer, cl_mem outputBuffer) {

        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputBuffer));
        clEnqueueNDRangeKernel(
                commandQueue,
                kernel,
                1,
                null,
                new long[]{ globalWorkSize },
                new long[]{ workItemSize / 2 },
                0,
                null,
                null);
    }


    private void getClMemoryBuffers(cl_context context, int[] input, int[] output, int[] sum, int workGroupCount) {
        inputBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * input.length, Pointer.to(input), null);
        outputBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * input.length, Pointer.to(output), null);
        sumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (workGroupCount + 1), Pointer.to(sum), null);
        scanSumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int * (workGroupCount + 1), Pointer.to(sum), null);
    }


    private cl_command_queue getClCommandQueue(cl_device_id device, cl_context context) {
        // Create a command-queue for the selected device
        cl_queue_properties queueProperties = new cl_queue_properties();
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

    public static void main(String args[]) {
        try {
            new Assignment2();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clean(cl_context context, cl_command_queue commandQueue, cl_program program, cl_kernel kernel) {
        // Release kernel, program, and memory objects
        clReleaseMemObject(inputBuffer);
        clReleaseMemObject(outputBuffer);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
