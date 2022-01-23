package fun.milkyway.bauthbridge.common.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

public class Utils {
    public static void exceptionWarningIntoLogger(Logger logger, Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        logger.warning(sw.toString());
    }
}
