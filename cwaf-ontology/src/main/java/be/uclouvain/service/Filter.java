package be.uclouvain.service;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.stream.Stream;

import org.apache.commons.cli.*;

import be.uclouvain.model.Directive;
import static be.uclouvain.utils.OntUtils.*;

public class Filter {

        public static int getDefaultPort(String scheme) {
            switch (scheme) {
                case "http":
                    return 80;
                case "https":
                    return 443;
                default:
                    return -1;
            }
        }

        public static Stream<Directive> filterFromRequest(Stream<Directive> order, HttpRequest request) {

        URI uri = request.uri();
        String path = uri.getPath();
        int port_tmp = uri.getPort();
        if (port_tmp == -1) {
            port_tmp = getDefaultPort(uri.getScheme());
        }
        final int port = port_tmp;
        
        return order.filter(d -> {
            return d.getLocation().match(path) && d.getVirtualHost().match(uri.getHost(), port);
        });
    }

    public static Stream<Directive> filter(Stream<Directive> order, String location, String host, int port) {
        return order.filter(d -> {
            return d.getLocation().match(location) && d.getVirtualHost().match(host, port);
        });
    }

    private static void run(String location, String host, int port, boolean all) {
        Stream<Directive> order = readStreamFromFile("global_order.ser");
        if (all) {
            order.forEach(System.out::println);
        } else {
            Stream<Directive> filtered = filter(order, location, host, port);
            filtered.forEach(System.out::println);
        }
    }
    
    public static void main(String[] args) {

            Options options = new Options();
            options.addOption("h", "help", false, "print this message");
            options.addOption("a", "all", false, "print all directives, ignore location and host if present");
            options.addOption("p", "port", true, "port");

            CommandLineParser parser = new DefaultParser();
            HelpFormatter formatter = new HelpFormatter();
            try {
                CommandLine cmd = parser.parse(options, args);

                int port = 80;
                if (cmd.hasOption("p")) {
                    try {
                        port = Integer.parseInt(cmd.getOptionValue("p"));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port number");
                        formatter.printHelp("Filter <host> <location>", options);
                        System.exit(1);
                    }
                }

                if (cmd.hasOption("h")) {
                    formatter.printHelp("Filter <host> <location>", options);
                } else if (cmd.hasOption("a")) {
                    run("", "", 80, true);
                } else {
                    // Handle the positional argument
                    String[] remainingArgs = cmd.getArgs();
                    if (remainingArgs.length != 2) {
                        System.out.println("Invalid number of arguments");
                        formatter.printHelp("Filter <host> <location>", options);
                        System.exit(1);
                    }

                    String host = remainingArgs[0];
                    String location = remainingArgs[1];
                    run(location, host, port, false);
                }

            } catch (ParseException e) {
                System.err.println("Parsing failed.  Reason: " + e.getMessage());
                formatter.printHelp("Filter <location> <host>", options);
                System.exit(1);
            }
    }
}
