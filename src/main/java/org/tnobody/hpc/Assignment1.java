package org.tnobody.hpc;

import org.apache.commons.io.FileUtils;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_mem;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadImage;
import static org.jocl.CL.clSetKernelArg;

/**
 * Created by tim on 17.05.2016.
 */
public class Assignment1 {

    final String KERNEL_FILE = "/RotateImage.c";

    private OpenCLContext openCLContext;
    private cl_mem inputImageMem;
    private cl_mem outputImageMem;

    public static void main(String[] args) throws IOException {
        new Assignment1();
    }

    public Assignment1() throws IOException {

        String programSource = FileUtils.readLines(new File(Assignment2.class.getResource(".").getPath() + KERNEL_FILE)).stream().collect(Collectors.joining(System.getProperty("line.separator")));

        BufferedImage image = createBufferedImage("homer.png");
        try {
            try {
                openCLContext = new OpenCLContext(programSource);
            } catch (Exception e) {
                e.printStackTrace();
            }
            IntStream.range(0,36).map(i -> i*10).forEach( i -> {

                cl_mem[] iOImages = openCLContext.getImageMem(image);
                inputImageMem = iOImages[0];
                outputImageMem = iOImages[1];

                BufferedImage rotatedImage = rotateImage((float) Math.toRadians(i), image.getWidth(), image.getHeight());
                try {
                    ImageIO.write(rotatedImage, "jpg", new File(getClass().getResource("").getPath() + "/rotated/rotated_" + i + ".jpg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            //BufferedImage rotatedImage = rotateImage(100, image.getWidth(), image.getHeight());
            //ImageIO.write(rotatedImage, "png", new File(getClass().getResource("").getPath() + "rotated.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BufferedImage rotateImage(float angle, int imageSizeX, int imageSizeY) {
        // Set up the work size and arguments, and execute the kernel
        long globalWorkSize[] = new long[2];
        globalWorkSize[0] = imageSizeX;
        globalWorkSize[1] = imageSizeY;

        clSetKernelArg(openCLContext.getKernel(), 0, Sizeof.cl_mem, Pointer.to(inputImageMem));
        clSetKernelArg(openCLContext.getKernel(), 1, Sizeof.cl_mem, Pointer.to(outputImageMem));
        clSetKernelArg(openCLContext.getKernel(), 2, Sizeof.cl_float, Pointer.to(new float[]{angle}));
        clEnqueueNDRangeKernel(openCLContext.getCommandQueue(), openCLContext.getKernel(), 2, null, globalWorkSize, null, 0, null, null);

        // Read the pixel data into the output image
        BufferedImage outputImage = new BufferedImage(imageSizeX, imageSizeY, BufferedImage.TYPE_INT_RGB);
        DataBufferInt dataBufferDst = (DataBufferInt) outputImage.getRaster().getDataBuffer();
        int dataDst[] = dataBufferDst.getData();

        clEnqueueReadImage(
                openCLContext.getCommandQueue(),
                outputImageMem,
                true,
                new long[3],
                new long[]{imageSizeX, imageSizeY, 1},
                imageSizeX * Sizeof.cl_uint,
                0,
                Pointer.to(dataDst),
                0,
                null,
                null);

        return outputImage;
    }

    private BufferedImage createBufferedImage(String imageName) {

        BufferedImage image = null;

        try {
            image = ImageIO.read(getClass().getResource(imageName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage rimage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = rimage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return rimage;
    }

}
