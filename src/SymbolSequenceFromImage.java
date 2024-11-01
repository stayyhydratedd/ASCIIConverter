import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class SymbolSequenceFromImage {
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    private final static File VIDEO_FILE = getVideoPath("enter the full path to the video: ");

    private final static VideoCapture VIDEO_CAPTURE = new VideoCapture(VIDEO_FILE.getPath());
    private final static double FPS = Math.round(VIDEO_CAPTURE.get(Videoio.CAP_PROP_FPS));
    private final static int FRAMES = (int) VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_COUNT);
    private final static int DELAY_BETWEEN_FRAMES = (int) (1000000 / FPS);

    private final static double FRAME_WIDTH = VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_WIDTH);
    private final static double FRAME_HEIGHT = VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_HEIGHT);

    private final static int WIDTH_SYMBOL_COUNT = 120;
    private final static double WIDTH_IN_PX =
            Math.floor(FRAME_WIDTH / WIDTH_SYMBOL_COUNT);
    private final static int HEIGHT_SYMBOL_COUNT = (int) (FRAME_HEIGHT / WIDTH_IN_PX * 0.55);
    private final static double HEIGHT_IN_PX = Math.floor(FRAME_HEIGHT / HEIGHT_SYMBOL_COUNT);

    private final static StringBuilder ASCII_FRAME = new StringBuilder();
//    private final static List<StringBuilder> ASCII_FRAME_LIST = new ArrayList<>();

    private final static char[] ASCII_SYMBOLS = {'@', '%', '#', '*', '+', '=', '-', ':', '.', ' '};
    private final static char[] ASCII_SYMBOLS_INVERT = {' ', '.', ':', '-', '=', '+', '*', '#', '%', '@'};

    public static void main(String[] args) throws IOException {

        System.out.println(FRAMES);
        System.out.println(FPS);
        System.out.println(DELAY_BETWEEN_FRAMES);
        System.out.println();
        System.out.println(FRAME_WIDTH);
        System.out.println(FRAME_HEIGHT);
        System.out.println();
        System.out.println(WIDTH_SYMBOL_COUNT);
        System.out.println(WIDTH_IN_PX);
        System.out.println(HEIGHT_SYMBOL_COUNT);
        System.out.println(HEIGHT_IN_PX);
//        getImageSequenceFromVideo();
    }

    protected static void start() throws IOException {

        List<String> ASCII_FRAME_LIST = new LinkedList<>();


        FFmpegFrameGrabber g = new FFmpegFrameGrabber(VIDEO_FILE);
        g.start();
        Java2DFrameConverter converter = new Java2DFrameConverter();

        for (int i = 0 ; i < FRAMES ; i++) {

            Frame frame = g.grabImage();
            BufferedImage bi = converter.convert(frame);

            Mat image = bufferedImage2Mat(bi);

            int currentCol = 0;
            int currentRow = 0;
            for(int j = 0; j < image.rows(); j++){
                if(j % HEIGHT_IN_PX == 0 && currentRow != HEIGHT_SYMBOL_COUNT){
                    for(int k = 0; k < image.cols(); k++){
                        if(k % WIDTH_IN_PX == 0){
                            if(currentCol == WIDTH_SYMBOL_COUNT){
                                currentCol = 0;
                                continue;
                            }
                            calculateGrayDepth(image.get(j, k)[0]);
                            currentCol++;
                        }
                        if(k == FRAME_WIDTH - 1)
                            ASCII_FRAME.append("\n");
                    }
                    currentRow++;
                }
            }
            ASCII_FRAME_LIST.add(ASCII_FRAME.toString());
            ASCII_FRAME.setLength(0);
        }
        long start = System.currentTimeMillis();
        for(String asciiFrame: ASCII_FRAME_LIST){
            System.out.print(asciiFrame);
            try {
                TimeUnit.MICROSECONDS.sleep(DELAY_BETWEEN_FRAMES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(System.currentTimeMillis() - start);
        g.stop();
    }

    private static void calculateGrayDepth(double pxValue){
        double brightnessStep = (double) 255 / 10;
        int pxBrightness = (int) (pxValue / brightnessStep) == 10?
                (int) (pxValue / brightnessStep - 1) : (int) (pxValue / brightnessStep);

        ASCII_FRAME.append(ASCIISymbol(() -> ASCII_SYMBOLS_INVERT[pxBrightness]));
    }
    private static Mat bufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_GRAYSCALE);
    }
    private static File getVideoPath(String message){
        System.out.print(message);
        String path = new Scanner(System.in).nextLine();
        File video = new File(path);
        if(video.exists())
            return video;
        else{
            System.out.println("video on this path doesn't exist");
            return getVideoPath("enter another path: ");
        }
    }
    private static char ASCIISymbol(Symbol s){
        return s.appendSymbolOnValue();
    }
}
@FunctionalInterface
interface Symbol{
    char appendSymbolOnValue();
}