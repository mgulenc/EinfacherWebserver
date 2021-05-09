import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class BasicWebserver extends Thread {
    private static final String LOG_FILE = "access.log";
    private static final int SOCKET_TIMEOUT = 30000;
    private final File root;
    private final boolean log;
    private final ServerSocket serverSocket;

    public BasicWebserver(File root, int port, boolean log) throws IOException {
        this.root = root.getCanonicalFile();
        this.log = log;
        System.out.println(this.root);
        if (!root.isDirectory()) {
            throw new IOException("No directory");
        }
        if (log)
            AccessLog.initializeLogger(new File(LOG_FILE));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
        serverSocket = new ServerSocket(port);
    }

    public void run() {
        while (true) {
            try {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                } catch (SocketException e) {
                    System.out.println(e.getMessage());
                    break;
                }

                socket.setSoTimeout(SOCKET_TIMEOUT);

                Request request = new Request(socket, root, log);
                request.start();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    public void shutdown() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (AccessLog.logger != null) {
                AccessLog.logger.close();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}