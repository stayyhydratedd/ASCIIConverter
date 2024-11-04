import java.io.*;
import java.util.Scanner;

public class SomeSettings {
    public static final String CURRENT_PATH = System.getProperty("user.dir");
    static{
        try {
            new File(CURRENT_PATH + "\\temp.txt").createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final File BAT_FILE = new File(CURRENT_PATH + "\\run.bat");
    public static final File TEMP_FILE = new File(CURRENT_PATH + "\\temp.txt");
    public static File VIDEO_FILE = getVideoFile("Enter video title (without .mp4): ");
    private static boolean IS_SAVE;

    protected static void start() {

        try (BufferedReader reader = new BufferedReader(new FileReader(TEMP_FILE))) {
            String videoName = reader.readLine(); //попытка прочесть имя видео в temp.txt
            if (videoName == null) { //если оно равно null
                writeToVideoTitleFile();    //записывает название видео в temp.txt
                writeToBat();   //перезаписывает размер окна в бат файле
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
                            SymbolSequenceFromImage.execution(VideoSaver.isSave());    //переменной файла присваивается файл с этим названием
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
            writer.write(VIDEO_FILE.getName().substring(0, VIDEO_FILE.getName().length() - 4));

            if(IS_SAVE)
                writer.write("\n/s");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeToBat(){
        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(BAT_FILE))) {

            bufferedWriter.write("@echo off\n" +
                    String.format("mode con:cols=%d lines=%d\n",
                            SymbolSequenceFromImage.WIDTH_SYMBOL_COUNT + 4,
                            SymbolSequenceFromImage.HEIGHT_SYMBOL_COUNT) +
                    "java -jar MediaToASCII.jar");

        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public static File getVideoFile(String message) {
        try(BufferedReader reader = new BufferedReader(new FileReader(TEMP_FILE))){
            String videoName = reader.readLine();
            if(videoName == null)
                return enterFileName(message);
            else
                return new File(videoName + "mp4");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File enterFileName(String message) {
        System.out.print(message);

        String videoName = new Scanner(System.in).nextLine();

        if(!checkInSavedVideo(videoName)){
            IS_SAVE = VideoSaver.needToSaveVideo("Save the resulting ASCII video (y/n)?");
        }

        File video = new File(videoName + ".mp4");
        if (video.exists())
            return video;
        else {
            return enterFileName("I can't find it with that title :(\nCheck the video title and enter again: ");
        }
    }

    public static boolean checkInSavedVideo(String videoName){
        File savedVideo = new File("savedASCIIVideos\\" + videoName + ".bin");
        return savedVideo.exists();
    }
}