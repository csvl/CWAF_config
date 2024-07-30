package be.uclouvain.service;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class ArcheoLogic {
    
    private static final Logger logger = LogManager.getLogger("ArcheoLogic");


    private static void run(String clue) {
        logger.debug("The clue is: " + clue);
    }

    public static void main(String[] args) {
            
            Options options = new Options();
            options.addOption("h", "help", false, "print this message");
            options.addOption("v", "version", false, "print the version information and exit");
            options.addOption("d", "debug", false, "enable debugging");
            
            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();
            try {
                CommandLine cmd = parser.parse(options, args);
                if (cmd.hasOption("d")) {
                    System.out.println("Debugging enabled");
                    Configurator.setRootLevel(org.apache.logging.log4j.Level.DEBUG);
                } else {
                    Configurator.setRootLevel(org.apache.logging.log4j.Level.INFO);
                }

                if (cmd.hasOption("h")) {
                    formatter.printHelp("ArcheoLogic <clue>", options);
                }
                else if (cmd.hasOption("v")) {
                    System.out.println("ArcheoLogic 1.0");
                }
                else {
                    // Handle the positional argument
                    String[] remainingArgs = cmd.getArgs();
                    if (remainingArgs.length < 1) {
                        System.out.println("Missing required argument: clue");
                        formatter.printHelp("utility-name <clue>", options);
                        System.exit(1);
                    }

                    String clue = remainingArgs[0];
                    run(clue);
                }

            } catch (ParseException e) {
                System.err.println("Parsing failed.  Reason: " + e.getMessage());
                formatter.printHelp("utility-name <name> [options]", options);
                System.exit(1);
            }
    }

}
