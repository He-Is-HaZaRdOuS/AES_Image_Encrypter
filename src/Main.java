import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;

public class Main {

    /* File names should be written without the file extension */
    public final static String FILE1 = "ex1";
    public final static String FILE2 = "ex2";

    public static void main(String[] args) throws NoSuchAlgorithmException {

        /* Create a secret key */
        SecretKey key = AESUtil.generateKey(256);

        try {
            /* Open the input image */
            File file1 = new File("res/" + FILE1 + ".png");

            /* Read PNG from IO */
            BufferedImage inputImage1 = ImageIO.read(file1);

            /* Encrypt example1 with ECB and CBC */
            genECBImage(cloneImage(inputImage1), key, FILE1);
            genCBCImage(cloneImage(inputImage1), key, FILE1);

        }
        catch (IOException e) {
            System.out.println("Ensure that `res/` folder is in the same directory as the jar file");
            System.out.println("And that FILE1's filename leads to a valid png without the file extension");
        }
        try {
            /* Open the input image */
            File file2 = new File("res/" + FILE2 + ".png");

            /* Read PNG from IO */
            BufferedImage inputImage2 = ImageIO.read(file2);

            /* Encrypt example2 with ECB and CBC */
            genECBImage(cloneImage(inputImage2), key, FILE2);
            genCBCImage(cloneImage(inputImage2), key, FILE2);

        } catch (IOException e) {
            System.out.println("Ensure that `res/` folder is in the same directory as the jar file");
            System.out.println("And that FILE2's filename leads to a valid png without the file extension");
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void genECBImage(BufferedImage input, SecretKey key, String filename) {
        try {
            int width = input.getWidth();
            int height = input.getHeight();
            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            ArrayList<Integer> shades = new ArrayList<>(width * height);

            /* Iterate over pixels */
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    /* Get pixel's color data and turn it to bytes */
                    int pixel = input.getRGB(x, y);
                    byte[] rgb = intToBytes(pixel);

                    /* Encrypt and convert encrypted byte data back to Integer */
                    byte[] encrypted_rgb = encryptECB(rgb, key);
                    int encrypted_pixel = bytesToInt(encrypted_rgb);
                    if(!shades.contains(encrypted_pixel))
                        shades.add(encrypted_pixel);

                    /* Write new pixel data to output image */
                    outputImage.setRGB(x, y, encrypted_pixel);
                }
            }
            /* Save to filesystem */
            saveImageAsPNG(outputImage, filename + "_ECB.png");

            System.out.print("Shades of gray used in " + filename + "_ECB.png: ");
            System.out.println(shades);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void genCBCImage(BufferedImage input, SecretKey key, String filename) {
        try {
            int width = input.getWidth();
            int height = input.getHeight();
            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

            /* Generate Initialization Vector for CBC Cipher */
            IvParameterSpec ivParameterSpec = AESUtil.generateIv();
            /* Byte array to store previous block data */
            byte[] predecessor = ivParameterSpec.getIV();

            /* Iterate over pixels */
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    /* Get pixel's color data and turn it to bytes */
                    int pixel = input.getRGB(x, y);
                    byte[] rgb = intToBytes(pixel);

                    /* XOR pixel data with previous block's data */
                    for (int k = 0; k < rgb.length; ++k) {
                        rgb[k] = (byte) (rgb[k] ^ predecessor[k]);
                    }

                    /* Encrypt and convert encrypted byte data back to Integer */
                    byte[] encrypted_rgb = encryptCBC(rgb, key, ivParameterSpec);
                    predecessor = encrypted_rgb;


                    int encrypted_pixel = bytesToInt(encrypted_rgb);
                    /* Write new pixel data to output image */
                    outputImage.setRGB(x, y, encrypted_pixel);
                }
            }
            /* Save to filesystem */
            saveImageAsPNG(outputImage, filename + "_CBC.png");

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Clones a BufferImage */
    public static BufferedImage cloneImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /* inits an ECB cipher and returns encrypted data */
    public static byte[] encryptECB(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    /* inits a CBC cipher and returns encrypted data */
    public static byte[] encryptCBC(byte[] data, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(data);
    }

    /* Break down integer to its respective BGR components (RGB but backwards because yes) */
    public static byte[] intToBytes(int value) {
        return new byte[] {
                (byte) 0xff000000,
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) (value & 0xff)
        };
    }

    /* Combine BGR components back */
    public static int bytesToInt(byte[] bytes) {
        return 0xff000000 |
                (0xff & bytes[1]) << 16 |
                (0xff & bytes[2]) << 8 |
                0xff & bytes[3];
    }

    /* Write BufferedImage data to filesystem as a PNG*/
    public static void saveImageAsPNG(BufferedImage outputImage, String filename) throws IOException {
        File outputFile = new File(filename);
        ImageIO.write(outputImage, "png", outputFile);
        System.out.println(filename + " created successfully.");
    }
}