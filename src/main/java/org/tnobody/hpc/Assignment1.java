package org.tnobody.hpc;

import com.sun.javaws.exceptions.InvalidArgumentException;
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
import java.util.stream.IntStream;

import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadImage;
import static org.jocl.CL.clSetKernelArg;

/**
 * Created by tim on 17.05.2016.
 */
public class Assignment1 {

    private static final String programSource =
            "" + "\n" +
                    "const sampler_t samplerIn = " + "\n" +
                    "    CLK_NORMALIZED_COORDS_FALSE | " + "\n" +
                    "    CLK_ADDRESS_CLAMP |" + "\n" +
                    "    CLK_FILTER_NEAREST;" + "\n" +
                    "" + "\n" +
                    "const sampler_t samplerOut = " + "\n" +
                    "    CLK_NORMALIZED_COORDS_FALSE |" + "\n" +
                    "    CLK_ADDRESS_CLAMP |" + "\n" +
                    "    CLK_FILTER_NEAREST;" + "\n" +
                    "" + "\n" +
                    "__kernel void rotateImage(" + "\n" +
                    "    __read_only  image2d_t sourceImage, " + "\n" +
                    "    __write_only image2d_t targetImage, " + "\n" +
                    "    float angle)" + "\n" +
                    "{" + "\n" +
                    "    int gidX = get_global_id(0);" + "\n" +
                    "    int gidY = get_global_id(1);" + "\n" +
                    "    int w = get_image_width(sourceImage);" + "\n" +
                    "    int h = get_image_height(sourceImage);" + "\n" +
                    "    int cx = w/2;" + "\n" +
                    "    int cy = h/2;" + "\n" +
                    "    int dx = gidX-cx;" + "\n" +
                    "    int dy = gidY-cy;" + "\n" +
                    "    float ca = cos(angle);" + "\n" +
                    "    float sa = sin(angle);" + "\n" +
                    "    int inX = (int)(cx+ca*dx-sa*dy);" + "\n" +
                    "    int inY = (int)(cy+sa*dx+ca*dy);" + "\n" +
                    "    inX = (int)(((cos(angle)*dx) - (sin(angle)*dy)) + cx);" + "\n" +
                    "    inY = (int)(((sin(angle)*dx) + (cos(angle)*dy)) + cy);" + "\n" +
                    "    int2 posIn = {inX, inY};" + "\n" +
                    "    int2 posOut = {gidX, gidY};" + "\n" +
                    "    uint4 pixel = read_imageui(sourceImage, samplerIn, posIn);" + "\n" +
                    "    write_imageui(targetImage, posOut, pixel);" + "\n" +
                    "}";

    private OpenCLContext openCLContext;
    private cl_mem inputImageMem;
    private cl_mem outputImageMem;

    public static void main(String[] args) {
        new Assignment1();
    }

    public Assignment1() {
        BufferedImage image = createBufferedImage("icelands_ring_road-wide.jpg");
        try {
            try {
                openCLContext = new OpenCLContext(programSource);
            } catch (InvalidArgumentException e) {
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
        clEnqueueNDRangeKernel(openCLContext.getCommandQueue(), openCLContext.getKernel(), 2, null,
                globalWorkSize, null, 0, null, null);

        // Read the pixel data into the output image
        BufferedImage outputImage = new BufferedImage(
                imageSizeX, imageSizeY, BufferedImage.TYPE_INT_RGB);
        DataBufferInt dataBufferDst =
                (DataBufferInt) outputImage.getRaster().getDataBuffer();
        int dataDst[] = dataBufferDst.getData();
        clEnqueueReadImage(
                openCLContext.getCommandQueue(), outputImageMem, true, new long[3],
                new long[]{imageSizeX, imageSizeY, 1},
                imageSizeX * Sizeof.cl_uint, 0,
                Pointer.to(dataDst), 0, null, null);

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
        g.drawImage(image,0,0,null);
        g.dispose();
        return rimage;
    }

}
