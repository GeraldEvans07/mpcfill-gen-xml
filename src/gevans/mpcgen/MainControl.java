
package gevans.mpcgen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Main class that handlers parsing the folder of pngs, generating the xml,
 * and tweaking the card images to display properly in MakePlayingCards.com
 * <p>
 * The cards downloaded through CardConjourer are not in a the dpi
 * that MPC wants. To counteract the image cropping they do, I add 72 
 * pixels around the border of the image. This replaces the image 
 * entirely.
 * 
 * @author Gerald Evans
 */
public class MainControl {

    /** The file name MPCFill is expecting */
    private static final String XML_FILE_NAME="cards.xml";

    /** Valid image types that MPC accepts */
    private static final List<String> VALID_FILE_TYPES = Arrays.asList(
        ".png",
        ".jpg",
        ".jpeg",
        ".bmp", 
        ".gif",
        ".tif",
        ".tiff"
    );

    /** The Purchase brackets that MPC has */
    private static final List<Integer> CARD_BRACKETS = Arrays.asList(
        18,
        36,
        55,
        72,
        90,
        108,
        126,
        144,
        162,
        80,
        198,
        216,
        234,
        396,
        504,
        612
    );

    /* Constructor variables */
    /** Config - can be null */
    private final ConfigParser config;

    /** The path where the {@link #XML_FILE_NAME} will be placed */
    private final Path outputPath;

    /** The path to read cards in from */
    private final Path cardPath;

    /** Map of every card read in from {@link #cardPath} */
    private final Map<Path, Integer> cardMap;

    /** The Card Back - can be null */
    private final String cardBack;

    /** Global slot variable to keep track of the slot in the file */
    private int slot;

    /**
     * Create a new MainControl object
     * 
     * @param outputPath the desired path to write the file too. If null, the directory
     *                   the process is running in is used.
     * @param cardPath  the desired path to read card files in from. If null, the directory
     *                  the process is running in is used.
     * @param configPath the desired config to use. If null, no config is used.
     */
    public MainControl(String outputPath, String cardPath, String configPath) {
        this.outputPath = getArgumentPathOrBase(outputPath).resolve(XML_FILE_NAME);
        this.cardPath = getArgumentPathOrBase(cardPath);
        if(configPath != null
                && Paths.get(configPath).toFile().exists()) {
            this.config = new ConfigParser(Paths.get(configPath));
            this.cardBack = config.getValue("CARD_BACK");
        }
        else {
            this.config = null;
            this.cardBack = "";
        }
        this.cardMap = new HashMap<>();
    }

    /**
     * Run the generator, truncating the file and reading the {@link #cardPath}
     * of all its image files. Then constructing the xml file.
     */
    public void runGenerator() {
        if(createFile()) {
            try {
                Files.walk(cardPath, 1).forEach(this::visitFile);
            }
            catch(IOException ioe) {
                System.err.printf("Failed to parse directory %s: %s\n", cardPath, ioe.getMessage());
                ioe.printStackTrace();
            }
            writeHeader();
            writeFronts();
            writeCardBack();
            writeFooter();
        }
        else {
            System.out.println("Failed to create the cards.xml file. If it already exists, check the permissions and try again.");
        }
    }

    
    /**
     * Used by the constructor to do a null check of the input to get
     * an absolute path to use.
     * 
     * @param arg A nullable string path to check
     * @return the absolute path of the input if it is not null,
     *         otherwise an absolute path of the currently running directory.
     */
    private Path getArgumentPathOrBase(String arg) {
        Path result;

        if(arg != null) {
            result = Paths.get(arg).toAbsolutePath();
        }
        else {
            result = Paths.get("").toAbsolutePath();
        }

        if(!result.toFile().isDirectory()) {
            System.err.printf("Unable to open %s\n", result.toAbsolutePath());
            throw new InvalidParameterException("Unable to open path: Directory does not exist");
        }

        return result;
    }

    /**
     * Attempt to create and or truncate the {@link #outputPath} file.
     * 
     * @return true if it sucessful, false otherwise.
     */
    private boolean createFile() {
        try {
            Files.writeString(outputPath, "");
            return true;
        }
        catch (IOException ioe) {
            System.err.printf("Failed to create file %s: %s\n",outputPath, ioe.getMessage());
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Lambda method for visiting a file during the walk.
     * <p>
     * If the file is an image and does not equal the card back, 
     * add the file to map and attempt to pull the number of slots
     * from the config file.
     * 
     * @param file the path being walked
     */
    private void visitFile(Path file) {
        String fileName = file.getFileName().toString();
        if(fileName.lastIndexOf('.') < 0) {
            return;
        }

        String fileType = fileName.substring(fileName.lastIndexOf('.'), fileName.length());

        if(!VALID_FILE_TYPES.contains(fileType)) {
            System.out.printf("Ignoring %s, not picture file.\n", fileName);
            return;
        }

        if(fileName.equals(cardBack)) {
            return;
        }

        /** 
         * Check for quantity value.
         * For laziness on the user. Check both the file name, and
         * the file name with the extension.
         */
        String cardName = fileName.substring(0, fileName.lastIndexOf('.'));
        int quantity = 1;
        if(config != null) {
            try {
                if(config.getValue(cardName) != null) {
                    quantity = Integer.parseInt(config.getValue(cardName));
                }
                else if (config.getValue(fileName) != null) {
                    quantity = Integer.parseInt(config.getValue(fileName));
                }
            }
            catch(NumberFormatException nfe) {
                System.err.printf("Failed to parse config for card %s, invalid number.\n", cardName);
                quantity = 1;
            }
            finally {
                if(quantity < 1) {
                    quantity = 1;
                }
            }
        }

        cardMap.put(file, quantity);

        tweakImage(file);
    }

    /**
     * Tweak the image to add black bars around the image.
     * <p>
     * Specifically checks if the image is 1500x2100 to only match images
     * downloaded from CardConjourer.
     * 
     * @param file the file to tewak
     */
    private void tweakImage(Path file) {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if(image.getWidth() == 1500 && image.getHeight() == 2100) {
                int border = 72;
                BufferedImage result = new BufferedImage(
                    image.getWidth() + (border * 2),
                    image.getHeight() + (border * 2),
                    image.getType()
                );
                Graphics2D g2d = (Graphics2D) result.getGraphics();
                g2d.setColor(Color.black);
                g2d.fillRect(0, 0, result.getWidth(), result.getHeight());
                g2d.drawImage(image, border -1, border -1, image.getWidth(), image.getHeight(), null);
                g2d.dispose();

                ImageIO.write(result,file.toString().substring(file.toString().lastIndexOf('.') + 1), file.toFile());
            }
        }
        catch(IOException ioe) {
            System.err.printf("Failed to tweak file %s: %s", file, ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /**
     * Attempt to write the header for the xml file
     * <p>
     * If there are no files or too many files, tell the user and exit out
     */
    private void writeHeader() {
        int cardTotal = cardMap.values().stream().reduce(0, Integer::sum);
        int bracket = CARD_BRACKETS.stream().filter(value -> value >= cardTotal).sorted().findFirst().orElse(0);

        if(bracket < CARD_BRACKETS.get(0)) {
            System.err.println("Unable to create order, too many cards in folder");
            System.exit(1);
        }

        writeLine("<order>");
        writeLine("\t<details>");
        writeLine(String.format("\t\t<quantity>%d</quantity>", cardTotal));
        writeLine(String.format("\t\t<bracket>%d</bracket>", bracket));
        writeLine("\t\t<stock>(S30) Standard Smooth</stock>");
        writeLine("\t\t<foil>false</foil>");
        writeLine("\t</details>");
    }

    /**
     * Write the front entry for each card
     */
    private void writeFronts() {
        slot = 0;

        writeLine("\t<fronts>");
        cardMap.forEach(this::writeCard);
        writeLine("\t</fronts>");
    }

    /**
     * Lambda method for writing card entries
     * 
     * @param file file to add
     * @param quantity quanitity of slots to add
     */
    private void writeCard(Path file, int quantity) {
        writeLine("\t\t<card>");
        writeLine(String.format("\t\t\t<id>%s</id>", file));
        String slots = "";
        for(int i = 0; i < quantity; i++) {
            slots += String.format("%d,", slot++);
        }
        /* Strip the last comma */
        slots = slots.substring(0, slots.length() - 1);
        writeLine(String.format("\t\t\t<slots>%s</slots>", slots));
        writeLine("\t\t</card>");
    }

    /**
     * Write each card back
     */
    private void writeCardBack() {
        writeLine("\t<backs>");
        writeLine("\t<!--");
        writeLine("\t\tMove any double sided card backs here.");
        writeLine("\t\tChange the slot value to the front card slot value");
        writeLine("\t\tChange the slot in the last card in <fronts> to the removed card's slot");
        writeLine("\t\tThen change the total quantity at the top to match the actual number of cards");
        writeLine("\t-->");
        writeLine("\t</backs>");
        writeLine(String.format("\t<cardback>%s</cardback>", cardPath.resolve(cardBack)));
    }

    /**
     * End the file
     */
    private void writeFooter() {
        writeLine("</order>");
    }

    /**
     * Writes a single line to the output file.
     * <p>
     * This method automatically appends a newline.
     * 
     * @param line the line to add
     */
    private void writeLine(String line) {
        try {
            Files.writeString(outputPath, String.format("%s\n", line), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        }
        catch(IOException ioe) {
            System.err.printf("Failed to write to file: %s\n", ioe.getMessage());
            ioe.printStackTrace();
        }
    }
}
