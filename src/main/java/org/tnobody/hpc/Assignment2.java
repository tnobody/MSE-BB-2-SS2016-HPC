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
    // Indices
    private static final int INPUT_DATA = 0;
    private static final int OUTPUT_DATA = 1;
    private static final int TEMPORARY_DATA = 2;
    public static final int SUM = 3;
    public static final int SCANNED_SUM = 4;

    // Data Init
    private static final long N = 8;// muss power of two sein
    private static final int USE_TEMP = -1;
    //long[] input = LongStream.range(0, N).map(l -> ThreadLocalRandom.current().nextInt(0, 10)).toArray();
    long[] input = LongStream.range(0, N).map(l -> 1).toArray();
    //long input[] = new long[] {3,1,7,0,4,1,6,3};
    long[] temp = new long[input.length];
    long[] output = new long[(int) N];
    long[] sums = new long[2];

    // OpenCl Config
    private static String programSource;
    final int PLATFORM_INDEX = 0;
    final long DEVICE_TYPE = CL_DEVICE_TYPE_GPU;
    final int DEVICE_INDEX = 0;
    final int WORKITEM_SIZE = 512; // JOCLDeviceQuery.main()

    // Kernel
    final String KERNEL_FILE = "/scan_kernel.c";
    final String SCAN_METHOD = "scan_work_efficient";
    final String ADD_METHOD = "add";
    private cl_mem inputBuffer;
    private cl_mem outputBuffer;
    private cl_mem tempBuffer;
    private cl_mem sumBuffer;
    private cl_mem scanSumBuffer;

    private Assignment2() throws IOException {
        programSource = FileUtils.readLines(new File(Assignment2.class.getResource(".").getPath() + KERNEL_FILE)).stream().collect(Collectors.joining(System.getProperty("line.separator")));
        // Create input- and output data
        Arrays.fill(temp, 0);

        // Enable exceptions and subsequently omit error checks in this sample
        long startJava = System.nanoTime();
        this.javaScan();
        System.out.println("Java took: " + (System.nanoTime() - startJava));

        long startOcl = System.nanoTime();
        this.oclScan();
        System.out.println("Ocl took: " + (System.nanoTime() - startOcl));

    }

    private void javaScan() {
        List<Long> out = JavaScan.scan((a,b) -> a+b, 0L,Arrays.stream(input).boxed().collect(Collectors.toList()));
        System.out.println("With Java" + out);
    }

    private void oclScan() {
        CL.setExceptionsEnabled(true);

        cl_platform_id platform = getClPlatformId();
        cl_device_id device = getClDeviceId(platform);
        cl_context context = getClContext(platform, device);
        cl_command_queue commandQueue = getClCommandQueue(device, context);

        cl_program program = clCreateProgramWithSource(context, 1, new String[]{programSource}, null, null);
        clBuildProgram(program, 0, null, null, null, null);
        cl_kernel scanKernel = clCreateKernel(program, SCAN_METHOD, null);
        cl_kernel addKernel = clCreateKernel(program, ADD_METHOD, null);

        // Set the arguments for the kernel
        // Allocate the memory objects for the input- and output data
        getClMemoryBuffers(context);

        executeScanKernel(commandQueue, scanKernel, N, inputBuffer, outputBuffer, sumBuffer);
        debug("After first", commandQueue);
        if (getWorkGroupCount() > 1) {
            executeScanKernel(commandQueue, scanKernel, getWorkGroupCount(), sumBuffer, sumBuffer, scanSumBuffer);
            debug("After second ", commandQueue);
            if (getWorkGroupCount() > WORKITEM_SIZE) {
                executeScanKernel(commandQueue, scanKernel,getWorkGroupCount(), scanSumBuffer, scanSumBuffer, null);
                debug("After third ", commandQueue);
            }
            clSetKernelArg(addKernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer)); // input array
            clSetKernelArg(addKernel, 1, Sizeof.cl_mem, Pointer.to(outputBuffer)); // output array
            clEnqueueNDRangeKernel(commandQueue, addKernel, 1, null, new long[]{N}, new long[]{WORKITEM_SIZE}, 0, null, null);
            debug("After add  ", commandQueue);
        }

        debug("After all", commandQueue);
        // Read the output data
        clean(context, commandQueue, program, scanKernel);
    }

    private void debug(String text, cl_command_queue commandQueue) {
        clEnqueueReadBuffer(commandQueue, outputBuffer, CL_TRUE, 0, N * Sizeof.cl_long, Pointer.to(output), 0, null, null);
        System.out.println("Input: " + text + Arrays.toString(input));
        System.out.println("Output: " + text + Arrays.toString(output));
    }

    private void executeScanKernel(cl_command_queue commandQueue, cl_kernel kernel, long workGroupCount, cl_mem inputBuffer, cl_mem outputBuffer, cl_mem sumBuffer) {
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputBuffer));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputBuffer));
        clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{(int) workGroupCount}));
        clSetKernelArg(kernel, 3, WORKITEM_SIZE * 2 * Sizeof.cl_int, null);
        if (sumBuffer == null) {
            clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(new cl_mem[getWorkGroupCount() + 1]));
        } else {
            clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(sumBuffer));
        }
        // zusätzlicher output array, der das letzte element jedes scans speichert => pro workgroup wieder aufsummiert
        // dafür nochmal einen eigenen kernel

        // Set the work-item dimensions
        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                new long[]{workGroupCount},
                new long[]{WORKITEM_SIZE > workGroupCount ?  workGroupCount / 2 : WORKITEM_SIZE / 2},
                0, null, null);
    }

    private void getClMemoryBuffers(cl_context context) {
        inputBuffer = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * N, Pointer.to(input), null);
        outputBuffer = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * N, Pointer.to(output), null);
        tempBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * N * 2, Pointer.to(temp), null);
        sumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * (getWorkGroupCount() + 1), Pointer.to(sums), null);
        scanSumBuffer = clCreateBuffer(context, CL_MEM_READ_WRITE | CL_MEM_COPY_HOST_PTR, Sizeof.cl_long * (getWorkGroupCount() + 1), Pointer.to(sums), null);
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
                contextProperties, 1, new cl_device_id[]{device},
                null, null, null);
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

    public int getWorkGroupCount() {
        return (int) ((N + WORKITEM_SIZE - 1) / WORKITEM_SIZE);
    }
}
