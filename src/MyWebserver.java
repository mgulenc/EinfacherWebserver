import java.io.File;
import java.io.IOException;

public class MyWebserver {

    public static void main(String[] args) {
        String root = args[0];
        int port = Integer.parseInt(args[1]);
        boolean log = Boolean.parseBoolean(args[2]);
        try{
            SimpleWebserver server = new SimpleWebserver(new File(root), port, log);
            server.start();
            System.out.println("MyWebserver gestartet..");
            System.out.println("ENTER oder Strg + C stoppt den Server.");
            System.in.read();
            System.exit(0);
        }catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

}
