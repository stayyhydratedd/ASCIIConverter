import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;

public class SomeSettings {
    public static final String CURRENT_PATH = System.getProperty("user.dir");
    static{
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            new File(CURRENT_PATH + "\\temp.txt").createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final File BAT_FILE = new File(CURRENT_PATH + "\\run.bat");
    public static final File TEMP_FILE = new File(CURRENT_PATH + "\\temp.txt");
    public static final String VIDEO_TITLE = getVideoTitle("Enter video title (without .mp4): ");
    public static File VIDEO_FILE ;
    private static boolean IS_SAVE;

    protected static void start() {

        try (BufferedReader reader = new BufferedReader(new FileReader(TEMP_FILE))) {
            String videoName = reader.readLine(); //попытка прочесть имя видео в temp.txt
            if (videoName == null) { //если оно равно null
                writeToVideoTitleFile();    //записывает название видео в temp.txt
                writeToBat(VIDEO_TITLE);   //перезаписывает размер окна в бат файле
                runNewCmdWindow();  //запускает новое окно
            } else {  //в новом окне уже выполнится это условие, так как videoName != null
                try (BufferedReader reader1 = new BufferedReader(new FileReader(TEMP_FILE))) {
                    String videoTitle = reader1.readLine();
                    if(checkInSavedVideo(videoTitle)){
                        SymbolSequenceFromImage.loadSavedASCIIVideo(videoTitle);
                        Thread showThread = new Thread(() -> SymbolSequenceFromImage.showASCIIVideo(false, true));
                        showThread.start();
                    } else {
                        try(BufferedReader reader2 = new BufferedReader(new FileReader(TEMP_FILE))){
                            VIDEO_FILE = new File(CURRENT_PATH + "\\" + reader2.readLine() + ".mp4");
                            SymbolSequenceFromImage.execution(VideoSaver.isSave(),
                                    new VideoCapture(SomeSettings.VIDEO_FILE.getPath()),
                                    VIDEO_FILE);    //переменной файла присваивается файл с этим названием
                            //и выполняется конвертация этого файла
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeToVideoTitleFile(){
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(TEMP_FILE, true))){
            writer.write(VIDEO_TITLE);

            if(IS_SAVE)
                writer.write("\n/s");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeToBat(String videoTitle){
        String savedVideoFilePath = "savedASCIIVideos\\" + videoTitle + ".bin";
        File videoFile = new File(videoTitle + ".mp4");

        VideoCapture videoCapture = new VideoCapture(videoFile.getPath());

        double frameWidth = videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double frameHeight = videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int widthSymbolCount = 120;
        int widthStepInPx = (int) (frameWidth / widthSymbolCount);
        int heightSymbolCount = (int) (frameHeight / widthStepInPx * 0.55);


        if(!new File(savedVideoFilePath).exists()) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(BAT_FILE))) {
                bufferedWriter.write("@echo off\n" +
                        String.format("mode con:cols=%d lines=%d\n",
                                widthSymbolCount + 1,
                                heightSymbolCount) +
                        "java -jar MediaToASCII.jar");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(savedVideoFilePath))) {
                LinkedList<String> frames = (LinkedList<String>) ois.readObject();
                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(BAT_FILE))) {
                    bufferedWriter.write("@echo off\n" +
                            String.format("mode con:cols=%d lines=%d\n",
                                    Integer.parseInt(frames.get(1)) + 1,
                                    Integer.parseInt(frames.get(2))) +
                            "java -jar MediaToASCII.jar");

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void setDefaultTitleAndBat(){

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(TEMP_FILE))) {
            writer.write("");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(BAT_FILE))) {

            writer.write("@echo off\n" +
                    String.format("mode con:cols=%d lines=%d\n", 70, 30) +
                    "java -jar MediaToASCII.jar");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void runNewCmdWindow(){
        String[] cmd = {"cmd", "/c", "start", BAT_FILE.getPath()};
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getVideoTitle(String message) {
        try(BufferedReader reader = new BufferedReader(new FileReader(TEMP_FILE))){
            String videoName = reader.readLine();
            if(videoName == null)
                return enterVideoTitle(message);
            else{
                return videoName;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String enterVideoTitle(String message) {
        System.out.print(message);

        String videoTitle = new Scanner(System.in).nextLine();

        if(!checkInSavedVideo(videoTitle)){
            IS_SAVE = VideoSaver.needToSaveVideo("Save the resulting ASCII video (y/n)?");
            if (checkInFolder(videoTitle)){
                return videoTitle;
            } else {
                return enterVideoTitle("I can't find it with that title :(\nCheck the video title and enter again: ");
            }
        }else
            return videoTitle;
    }
    private static boolean checkInFolder(String videoTitle){
        File video = new File(videoTitle + ".mp4");
        return video.exists();
    }
    public static boolean checkInSavedVideo(String videoName){
        File savedVideo = new File("savedASCIIVideos\\" + videoName + ".bin");
        return savedVideo.exists();
    }
}