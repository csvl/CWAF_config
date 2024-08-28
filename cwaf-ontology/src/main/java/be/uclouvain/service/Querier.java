package be.uclouvain.service;

import org.apache.commons.cli.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.ModelFactory;

public class Querier {
    
    public static String loadQueryFromFile(String queryPath) {
        //Load the query
        StringBuilder queryString = new StringBuilder();
        try {
            File myfile = new File(queryPath);
            Scanner myReader = new Scanner(myfile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                queryString.append(data).append("\n");
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error reading query file: " + e.getMessage());
            System.exit(1);
        }
        return queryString.toString();
    }

    public static void runFromString(String ontologyPath, String queryString) {
        //Load the ontology
        OntModel model = ModelFactory.createOntologyModel();
        model.read(ontologyPath);
        
        //Query
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            ResultSetFormatter.out(System.out, results);
        } finally {
            qexec.close();
        }
    }

    public static void runFromFile(String ontologyPath, String queryPath) {
        //Query
        String queryString = loadQueryFromFile(queryPath); 
        runFromString(ontologyPath, queryString);
    }

    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Querier <ontology path> <query path>", options);
    }
    
    //Do a SPARQL query on the ontology
    public static void main(String[] args) {

            Options options = new Options();
            options.addOption("h", "help", false, "print this message");
            options.addOption("q", "query", true, "Use a string as query instead of a file");

            CommandLineParser parser = new DefaultParser();
            try {
                CommandLine cmd = parser.parse(options, args);

                if (cmd.hasOption("h")) {
                    printHelp(options);
                } else if (cmd.hasOption("q")){
                    String ontologyPath = cmd.getArgs()[0];
                    String queryString = cmd.getOptionValue("q");
                    runFromString(ontologyPath, queryString);
                } else {
                    // Handle the positional argument
                    String[] remainingArgs = cmd.getArgs();
                    if (remainingArgs.length != 2) {
                        System.out.println("Invalid number of arguments");
                        printHelp(options);
                        System.exit(1);
                    }
                    
                    String ontologyPath = remainingArgs[0];
                    String queryPath = remainingArgs[1];

                    runFromFile(ontologyPath, queryPath);

                }

            } catch (ParseException e) {
                System.err.println("Parsing failed.  Reason: " + e.getMessage());
                printHelp(options);
                System.exit(1);
            }

        
    }
}
