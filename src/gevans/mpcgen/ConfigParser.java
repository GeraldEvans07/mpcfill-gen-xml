
package gevans.mpcgen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses the config file for the MainControl
 * <p>
 * This is extremly insecure parser. Maping strings to strings.
 * 
 * @author Gerald Evans
 */
public class ConfigParser {

    private final Map<String, String> configMap;

    /**
     * Construct a new config parser
     * 
     * @param config the path to parse
     */
    public ConfigParser(Path config) {
        configMap = new HashMap<>();
        readFile(config);
    }

    /**
     * Retrieve a value from the config map
     * <p>
     * The key is not case sensitive
     * 
     * @param key the key to find
     * @return the value for the key, or null if it does not exist
     */
    public String getValue(String key) {
        return configMap.get(key.toUpperCase());
    }

    /**
     * Read each line of the file, putting valid objects into the map
     * 
     * @param config the path to read in
     */
    private void readFile(Path config) {
        try{
            List<String> lines = Files.readAllLines(config);
            lines.forEach(this::readLine);
        }
        catch(IOException ex) {
            System.err.printf("Failed to read config file: %s\n", ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Reads a single line of the file. This method's only checks are
     * splitting on an = sign and comments. 
     * <p>
     * Keys are stored in uppercase
     * 
     * @param line the line to parse
     */
    private void readLine(String line) {
        if(line.startsWith("#")) {
            return;
        }
        String[] tok = line.split("=");
        if(tok.length != 2) {
            System.err.printf("Failed to parse line: %s\n", line);
            return;
        }

        configMap.put(tok[0].trim().toUpperCase(),tok[1].trim());
    }
}
