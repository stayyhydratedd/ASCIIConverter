import java.io.*;
import java.util.List;
import java.util.Scanner;

public class VideoSaver {

    public static boolean needToSaveVideo(String message){
        System.out.print(message);
        String answer = new Scanner(System.in).next();
        if(!answer.matches("[ynYN]"))
            return needToSaveVideo("Please answer with one letter (y-yes/n-no): ");
        else return answer.matches("[yY]");
    }

    public static void saveFile(List<String> ASCIIFrameList){
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SomeSettings.CURRENT_PATH +
                    "\\savedASCIIVideos\\" +
                    SomeSettings.VIDEO_FILE.getName()
                            .substring(0, SomeSettings.VIDEO_FILE.getName().length() - 4) + ".bin"))){

            oos.writeObject(ASCIIFrameList);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSave(){
        try (BufferedReader reader = new BufferedReader(new FileReader(SomeSettings.TEMP_FILE))) {
            reader.readLine();
            String s = reader.readLine();
            if(s != null)
                if(s.matches("/s"))
                    return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}