import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SymbolSequenceFromImage {

    private final static Object LOCK = new Object();

    public static File VIDEO_FILE;
    public static VideoCapture VIDEO_CAPTURE;
    private static double FPS;
    private static int FRAMES;
    private static int DELAY_BETWEEN_FRAMES;

    private static double FRAME_WIDTH;
    private static double FRAME_HEIGHT;

    public static int WIDTH_SYMBOL_COUNT;
    private static int WIDTH_STEP_IN_PX;
    public static int HEIGHT_SYMBOL_COUNT;
    private static int HEIGHT_STEP_IN_PX;

    private static List<String> ASCII_FRAME_LIST = new LinkedList<>();
    private final static StringBuilder ASCII_FRAME = new StringBuilder();
    private static FFmpegFrameGrabber FRAME_GRABBER;
    private final static Java2DFrameConverter CONVERTER = new Java2DFrameConverter();

//    private final static char[] ASCII_SYMBOLS = {'@', '%', '#', '*', '+', '=', '-', ':', '.', ' '};
//    private final static char[] ASCII_SYMBOLS_INVERT = {' ', '.', ':', '-', '=', '+', '*', '#', '%', '@'};
    private final static char[] ASCII_SYMBOLS_CUSTOM = {' ', '.', ':', '!', '-', '~', '=', '+', '*', '#', '%', '$', '@'};

    public static void main(String[] args) {
        printInfo();
    }

    public static void execution(boolean needToSave, VideoCapture videoCapture, File videoFile) {
        VIDEO_FILE = videoFile;
        VIDEO_CAPTURE = videoCapture;
        initializeVideoParameters();
        runThreads(needToSave);
        SomeSettings.setDefaultTitleAndBat();
        showASCIIVideo(needToSave, false);
    }
    private static void initializeVideoParameters(){
        FPS = Math.round(VIDEO_CAPTURE.get(Videoio.CAP_PROP_FPS));
        FRAMES = (int) VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_COUNT);
        DELAY_BETWEEN_FRAMES = (int) (1000000 / FPS);
        FRAME_WIDTH = VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_WIDTH);
        FRAME_HEIGHT = VIDEO_CAPTURE.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        WIDTH_SYMBOL_COUNT = 120;
        WIDTH_STEP_IN_PX = (int) (FRAME_WIDTH / WIDTH_SYMBOL_COUNT);
        HEIGHT_SYMBOL_COUNT = (int) (FRAME_HEIGHT / WIDTH_STEP_IN_PX * 0.55);
        HEIGHT_STEP_IN_PX = (int) (FRAME_HEIGHT / HEIGHT_SYMBOL_COUNT);
        FRAME_GRABBER = new FFmpegFrameGrabber(VIDEO_FILE);
    }

    private static void runThreads(boolean needToSave){
        runConvertingThread(needToSave);
        loadingThread().start();
    }

    private static void runConvertingThread(boolean needToSave) {
        Thread thread = new Thread(() -> {
            try {
                addASCIIFrameToList(needToSave);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }

    private static Mat bufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_GRAYSCALE);
    }

    private static void addASCIIFrameToList(boolean needToSave) throws IOException {

        FRAME_GRABBER.start();
        if(needToSave) {
            ASCII_FRAME_LIST.add(Integer.toString(DELAY_BETWEEN_FRAMES));
            ASCII_FRAME_LIST.add(Integer.toString(WIDTH_SYMBOL_COUNT));
            ASCII_FRAME_LIST.add(Integer.toString(HEIGHT_SYMBOL_COUNT));
        }
        for (int i = 0; i < FRAMES; i++) {
            Frame frame = FRAME_GRABBER.grabImage();
            BufferedImage bi = CONVERTER.convert(frame);
            Mat image = bufferedImage2Mat(bi);

            int currentCol = 0;
            int currentRow = 0;
            for (int j = 0; j < FRAME_HEIGHT; j++) {
                if (j % HEIGHT_STEP_IN_PX == 0 && currentRow != HEIGHT_SYMBOL_COUNT) {
                    if(currentRow < HEIGHT_SYMBOL_COUNT) {
                        for (int k = 0; k < FRAME_WIDTH; k++) {
                            if (k % WIDTH_STEP_IN_PX == 0) {
                                if (currentCol < WIDTH_SYMBOL_COUNT) {
                                    ASCII_FRAME.append(calculateGrayDepth(image.get(j, k)[0], ASCII_SYMBOLS_CUSTOM));
                                    currentCol++;
                                } else {
                                    currentCol = 0;
                                    continue;
                                }
                            }
                            if (k + 4 == WIDTH_SYMBOL_COUNT * WIDTH_STEP_IN_PX) {
                                ASCII_FRAME.append("\n");
                                break;
                            }
                        }
                        currentRow++;
                    }
                }
            }
            ASCII_FRAME_LIST.add(ASCII_FRAME.toString());
            ASCII_FRAME.setLength(0);
        }
        FRAME_GRABBER.stop();
    }

    private static char calculateGrayDepth(double pxValue, char[] ASCIIChars) {
        double brightnessStep = (double) 255 / ASCIIChars.length;
        int pxBrightness = (int) (pxValue / brightnessStep) == ASCIIChars.length ?
                (int) (pxValue / brightnessStep - 1) : (int) (pxValue / brightnessStep);

        return ASCIISymbol(() -> ASCIIChars[pxBrightness]);
    }

    private static Thread loadingThread() {
        return new Thread(SymbolSequenceFromImage::printLoading);
    }

    private static void printLoading() {
        synchronized (LOCK) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.print("===================\nConverting to ASCII...\n Converted frames:");
            while (ASCII_FRAME_LIST.size() < FRAMES) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.printf("\n  %d of %d", ASCII_FRAME_LIST.size(), FRAMES);
            }
            LOCK.notify();
        }
    }
    public static void showASCIIVideo(boolean needToSave, boolean savedFile) {
        synchronized (LOCK) {
            if(needToSave) {
                VideoSaver.saveFile(ASCII_FRAME_LIST);
            }
            if(!savedFile)
                System.out.println("\n===================\nConverting is done, press Enter to continue..");
            else
                System.out.println("Your ASCII file ready, press Enter to continue..");

            new Scanner(System.in).nextLine();

            while (true) {
                for (String ASCIIFrame : ASCII_FRAME_LIST) {
                    System.out.print(ASCIIFrame);
                    try {
                        if(savedFile) {
                            int delayBetweenFrames = Integer.parseInt(ASCII_FRAME_LIST.getFirst());
                            TimeUnit.MICROSECONDS.sleep(delayBetweenFrames);
                        }
                        else
                            TimeUnit.MICROSECONDS.sleep(DELAY_BETWEEN_FRAMES);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
    public static void loadSavedASCIIVideo(String videoTitle){
        SomeSettings.setDefaultTitleAndBat();
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("savedASCIIVideos\\" + videoTitle + ".bin"))){
            ASCII_FRAME_LIST = (LinkedList<String>) ois.readObject();
            showASCIIVideo(false, true);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private static void printInfo(){
        System.out.printf("""
                Frames: %d
                FPS: %f
                Delay between frames (in microseconds): %d
                
                Frame width: %f
                Frame height: %f
                
                Width symbol count: %d
                Width in px between symbols: %d
                Height symbols count: %d
                Height in px between symbols: %d
                """, FRAMES, FPS, DELAY_BETWEEN_FRAMES, FRAME_WIDTH, FRAME_HEIGHT,
                WIDTH_SYMBOL_COUNT, WIDTH_STEP_IN_PX, HEIGHT_SYMBOL_COUNT, HEIGHT_STEP_IN_PX);
    }
    private static char ASCIISymbol(Symbol s){
        return s.appendSymbolOnValue();
    }
}
@FunctionalInterface
interface Symbol{
    char appendSymbolOnValue();
}