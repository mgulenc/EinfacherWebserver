import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class SimpleWebserver extends Thread {
    private static final String LOG_FILE = "access.log";        //Filename, Contains information about accesses
    private static final int SOCKET_TIMEOUT = 30000;
    private File Root;
    private ServerSocket Server_Socket;
    private final Boolean Log;

    public SimpleWebserver(File root, int port, Boolean log) throws IOException {

        Root = root.getCanonicalFile();                  //Reference to index.html
        System.out.println(Root);                       //sends index.html
        if (!Root.isDirectory()) {
            throw new IOException("No directory");
        }

        Server_Socket = new ServerSocket(port);

        //If Log is true, the access will be written in the access.log file
        Log = log;
        if (Log)
            AccessLog.initializeLogger(new File(LOG_FILE));

        //Starts the Server and adds an event-handler to shut down correctly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    public void run() {
        while (true) {
            try {
                Socket socket = Server_Socket.accept();
                socket.setSoTimeout(SOCKET_TIMEOUT);

                Request request = new Request(socket, Root, Log);
                request.start();
            }
            catch (SocketException e) {
                System.out.println(e.getMessage());
                break;
            }
            catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void shutdown() {
        try {
            if (Server_Socket != null) {
                Server_Socket.close();
            }
            if (AccessLog.logger != null) {
                AccessLog.logger.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}