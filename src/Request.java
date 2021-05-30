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
    private final Socket Socket;
    private final File Root;
    private final Boolean Log;
    private String Path;
    private File IndexFile;
    private BufferedOutputStream Out;


    public Request(Socket socket, File root, Boolean log) throws IOException {
        Socket = socket;
        Root = root;
        Log = log;
    }

    public void run() {
        try {
            InputStreamReader reader = new InputStreamReader(Socket.getInputStream());
            BufferedReader in = new BufferedReader(reader);
            String request = in.readLine(); //URL from Browser
            Out = new BufferedOutputStream(Socket.getOutputStream());

            if (request == null || request.trim().length() == 0)
                return;
            if (Log)
                writeAccess(request);

            //Just GET is implemented
            if (!request.startsWith("GET")) {
                sendError(Out, Status.NOT_IMPLEMENTED, "Just the GET HTTP-Method is implemented.");
                return;
            }

            //Other queries in the URL will be ignored
            Path = request.substring(4, request.length() - 9);//http: ignored
            int idx = Path.indexOf(request);
            if (idx >= 0) {
                Path = Path.substring(0, idx);
            }

            //calls the index.html file
            IndexFile = new File(Root, URLDecoder.decode(Path, StandardCharsets.UTF_8)).getCanonicalFile();

            if (!checkFile())
                return;

            //Loads file
            InputStream is = new BufferedInputStream(new FileInputStream(IndexFile));
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(IndexFile.getName());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            sendHeader(Out, Status.OK, contentType, IndexFile.length(), IndexFile.lastModified());

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                Out.write(buffer, 0, bytesRead);
            }
            Out.flush();
            in.close();
            Socket.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Time out! " + e.getMessage());
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeAccess(String request) throws IOException {
        try {
            AccessLog.logger.log("[" + simpleDateFormat.format(new Date())
                    + " " + Socket.getInetAddress().getHostAddress() + ":"
                    + Socket.getPort() + "] " + request);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


    } //writes information about the request in access.log

    private Boolean checkFile() throws IOException {
        //Checks that path isn't a directory
        if (IndexFile.isDirectory()) {
            File indexFile = new File(IndexFile, "index.html");
            if (indexFile.exists() && !indexFile.isDirectory()) {
                IndexFile = indexFile;
            } else {
                sendError(Out, Status.FORBIDDEN, "File doesn't exist or the path is wrong");
                return false;
            }
        }

        //Access outside root is not permitted
        if (!IndexFile.getCanonicalPath().startsWith(Root.getCanonicalPath())) {
            sendError(Out, Status.FORBIDDEN, "You can't have access to this file. Ask your admin for more information.");
            return false;
        }

        //Checks file existence
        if (!IndexFile.exists()) {
            sendError(Out, Status.NOT_FOUND, "The file doesn't exist.");
            return false;
        }

        return true;
    }

    private void sendError(BufferedOutputStream out, Status status, String reason)
            throws IOException {
        String msg = status.getMessage() + " : " + reason;
        sendHeader(out, status, "text/html", msg.length(), System.currentTimeMillis());
        out.write(msg.getBytes());
        out.flush();
    }

    private void sendHeader(BufferedOutputStream out, Status status,
                            String contentType, long length, long time) throws IOException {
        String header =
                "HTTP/1.1 " + status.getCode() + " " + status.getMessage()
                        + "\r\nDate: " + getTime(System.currentTimeMillis())
                        + "\r\nServer: BasicWebserver"
                        + "\r\nContent-Type: " + contentType
                        + "\r\nContent-Length: " + length
                        + "\r\nLast-Modified: " + getTime(time)
                        + "\r\nConnection: close"
                        + "\r\n\r\n";
        out.write(header.getBytes());
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