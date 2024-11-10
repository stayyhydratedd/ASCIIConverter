import org.opencv.core.Mat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageToASCII{
    private static Mat IMAGE;
    public static int WIDTH_SYMBOLS;
    public static int HEIGHT_SYMBOLS;

    public static void start(String videoTitle){
        File imageFile = new File(SomeSettings.CURRENT_PATH + "\\" + videoTitle);
        try {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            IMAGE = VideoToASCII.bufferedImage2Mat(bufferedImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        createASCIIImage();
        SomeSettings.setDefaultTitleAndBat();
    }
    private static void setWidthSymbols(){
        try(BufferedReader reader = new BufferedReader(new FileReader(SomeSettings.TEMP_FILE))) {
            reader.readLine();
            WIDTH_SYMBOLS = Integer.parseInt(reader.readLine());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void createASCIIImage(){
        setWidthSymbols();
        int imageWidth = IMAGE.cols();
        int imageHeight = IMAGE.rows();
        int widthStepInPx = imageWidth / WIDTH_SYMBOLS;
        HEIGHT_SYMBOLS = (int) (imageHeight / widthStepInPx * 0.55);
        int heightStepInPx = imageHeight / HEIGHT_SYMBOLS;

        StringBuilder ASCIIImage = new StringBuilder();

        int currentCol = 0;
        int currentRow = 0;
        for (int j = 0; j < imageHeight; j++) {
            if (j % heightStepInPx == 0 && currentRow != HEIGHT_SYMBOLS) {
                if(currentRow < HEIGHT_SYMBOLS) {
                    for (int k = 0; k < imageWidth; k++) {
                        if (k % widthStepInPx == 0) {
                            if (currentCol < WIDTH_SYMBOLS) {
                                ASCIIImage.append(VideoToASCII.calculateGrayDepth(IMAGE.get(j, k)[0], VideoToASCII.ASCII_SYMBOLS_CUSTOM));
                                currentCol++;
                            } else {
                                currentCol = 0;
                                continue;
                            }
                        }
                        if (k + 4 == WIDTH_SYMBOLS * widthStepInPx) {
                            ASCIIImage.append("\n");
                            break;
                        }
                    }
                    currentRow++;
                }
            }
        }
        System.out.println(ASCIIImage);
    }
}
