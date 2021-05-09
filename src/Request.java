import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Request extends Thread {
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    private final File root;
    private final boolean log;
    private final Socket socket;

    public Request(Socket socket, File root, boolean log) {
        this.socket = socket;
        this.root = root;
        this.log = log;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(
                     socket.getOutputStream())) {
            String request = in.readLine();
            if (request == null || request.trim().length() == 0)
                return;
            if (log) {
                AccessLog.logger.log("[" + simpleDateFormat.format(new Date())
                        + " " + socket.getInetAddress().getHostAddress() + ":"
                        + socket.getPort() + "] " + request);
            }
            if (!request.startsWith("GET")) {
                sendError(out, Status.NOT_IMPLEMENTED,
                        "Nur die HTTP-Methode GET ist implementiert.");
                return;
            }
            //Query Strings werden ignoriert.
            String path = getPath(request);
            int idx = path.indexOf(request);
            if (idx >= 0) {
                path = path.substring(0, idx);
            }
            File file = new File(root, URLDecoder.decode(path, StandardCharsets.UTF_8)).getCanonicalFile();
            if (file.isDirectory()) {
                File indexFile = new File(file, "index.html");
                if (indexFile.exists() && !indexFile.isDirectory()) {
                    file = indexFile;
                } else {
                    sendError(out, Status.FORBIDDEN, "");
                }
            }
            //Zugriff außerhalb von root ist nicht erlaubt.
            if (!file.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                sendError(out, Status.FORBIDDEN, "");
                return;
            }
            if (!file.exists()) {
                sendError(out, Status.NOT_FOUND, "");
                return;
            }
            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                String contentType = URLConnection.getFileNameMap().getContentTypeFor(file.getName());

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                sendHeader(out, Status.OK, contentType, file.length(), file.lastModified());

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();
        } catch (SocketTimeoutException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void sendError(BufferedOutputStream out, Status status, String reason)
            throws IOException {
        String msg = status.getMessage() + ":" + reason;
        sendHeader(out, status, "text/html", msg.length(),
                System.currentTimeMillis());
        out.write(msg.getBytes());
        out.flush();
    }

    private void sendHeader(BufferedOutputStream out, Status status,
                            String contentType, long length, long time) throws IOException {
        String header =
                "HTTP/1.1 " + status.getCode() + " " + status.getMessage()
                        + "\r\nDate: " + getTime(System.currentTimeMillis())
                        + "\r\nServer: SimpleWebserver"
                        + "\r\nContent-Type: " + contentType
                        + "\r\nContent-Lenght: " + length
                        + "\r\nLast-Modified: " + getTime(time)
                        + "\r\nConnection: close"
                        + "\r\n\r\n";
        out.write(header.getBytes());
    }

    private static String getPath(String request) {
        return request.substring(4, request.length() - 9);
    }

    private static String getTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}
