/*---------------------------- UNCLASSIFIED/FOUO ----------------------------*/

/*=====================Begin Data Rights Header======================

   This license applies to this file, the files found in this
   directory and its sub-directories unless stated otherwise within
   that sub-directory.

   SBIR Data Rights
        Contract No.            N00024-19-C-6267
        Contractor Name         Progeny Systems Corporation
        Contractor Address      9500 Innovation Drive
                                Manassas, VA 20110
        Expiration of SBIR
        Data Rights Period:     June 17, 2039, Subject to the SBIR Policy
                                Directive effective May 2, 2019

   The Government's rights to use, modify, reproduce, release,
   perform, display, or disclose technical data or computer software
   marked with this legend are restricted during the period shown as
   provided in paragraph (b)(4) of the Rights in Noncommercial
   Technical Data and Computer Software -- Small Business Innovative
   Research (SBIR) Program clause contained in the above identified
   contract.  No restrictions apply after the expiration date shown
   above.  Any reproduction of technical data, computer software, or
   portions thereof marked with this legend must also reproduce the
   markings.

  =======================End Data Rights Header======================*/

// Copyright - Progeny Systems Corporation

package gevans.mpcgen;

/**
 * Entry point of the MPCFillGenerator
 * 
 * @author Gerald Evans
 */
public class Main {

    public static void main(String ... args) {
        String outputPath = null;
        String cardPath = null;
        String config = null;
        for(int i = 0; i < args.length; i++) {
            if(!args[i].startsWith("-")){
                System.err.printf("Invalid format: %s\n", args[i]);
                continue;
            }

            switch(args[i].toLowerCase()) {
                case "-out":
                    outputPath = args[++i];
                    break;
                case "-cards":
                    cardPath = args[++i];
                    break;
                case "-cfg":
                    config = args[++i];
                    break;
                case "-help":
                default:
                    printHelp();
                    System.exit(0);
            }
        }

        MainControl generator = new MainControl(outputPath, cardPath, config);
        generator.runGenerator();
    }

    private static void printHelp() {
        System.out.printf(
            "Arguments:\n" +
            "\t-out [file-path]\t: specify the path of the output file\n" +
            "\t-cards [file-path]\t: specify the path where the card image files exist\n" +
            "\t-cfg [file-name]\t: specify the config file to use\n" +
            "\t-help\t\t\t: print this page\n" 
        );
    }
}
