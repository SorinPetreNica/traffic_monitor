package traffic.monitor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class FileUtil {

    private static final Logger LOG = Logger.getLogger(FileUtil.class);

    public interface LineParser<T> {
        T deserialize(String[] tokens);
    }

    public static <T> List<T> parseFileLineByLine(String filePath, String tokenSeparator, LineParser<T> lineParser) {
        File file = new File(filePath);
        LOG.info("Opening file :: " + file.getAbsolutePath());
        InputStream fileInputStream = FileUtil.class.getClassLoader().getResourceAsStream(filePath);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream))) {
            List<T> deserializedObjects = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(tokenSeparator);
                T deserializedObject = lineParser.deserialize(tokens);
                if (deserializedObject != null) {
                    deserializedObjects.add(deserializedObject);
                }
            }
            return deserializedObjects;
        } catch (IOException e) {
            LOG.error("Error parsing file " + filePath, e);
            throw new RuntimeException(e);
        }
    }
}
