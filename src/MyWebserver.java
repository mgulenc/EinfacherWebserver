import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;


public class MyWebserver {

    public static File Root;
    public static int Port;
    public static Boolean Log;

    public static void main(String[] args) {

        //Load configuration
        File configFile = new File("src/config.properties");
        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            String rootString = props.getProperty("root");
            Root = new File(rootString); //Reference to directory web, contains HTML-files
            Port = Integer.parseInt(props.getProperty("port")); //default: 80
            Log = Boolean.parseBoolean(props.getProperty("log")); //default: true

            reader.close();

            //Start Webserver
            SimpleWebserver server = new SimpleWebserver(Root, Port, Log);
            server.start();
            System.out.println("MyWebserver has started..");
            System.out.println("ENTER oder ctrl + c to stop the server.");
            System.in.read();
            System.exit(0);

        } catch (FileNotFoundException ex) {
            System.err.println("There was an error with the configuration file: " + ex.getMessage());
        } catch (IOException e){
            System.err.println(e.getMessage());
        }
    }
}
