import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AccessLog {
    public static AccessLog logger;
    private final BufferedWriter logFileWriter;

    private AccessLog(File logfile) throws IOException {
        logFileWriter = new BufferedWriter(new FileWriter(logfile, true));
    }

    public static void initializeLogger(File logfile) throws IOException {
        if (logger == null)
            logger = new AccessLog(logfile);
    }

    public void log(String info) throws IOException {
        synchronized (logFileWriter) {
            logFileWriter.write(info);
            logFileWriter.newLine();
            logFileWriter.flush();

            System.out.println(info);
        }
    }

    public void close() {
        try {
            logFileWriter.flush();
            logFileWriter.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}