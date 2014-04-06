package traffic.monitor.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DateUtil {

    private static final Logger     LOG            = Logger.getLogger(DateUtil.class);

    private static final DateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static Date parseDate(String string) {
        try {
            return fileDateFormat.parse(string);
        } catch (ParseException e) {
            LOG.error("Unable to parse string :: " + string);
            throw new RuntimeException(e);
        }
    }

}
