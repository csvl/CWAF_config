# WAF-GUARD

Welcome to the official GitHub repository for **WAF-GUARD**, a powerful tool designed to assist in troubleshooting Web Application Firewall (WAF) configurations by leveraging an ontological representation.
This repository contains the source code and documentation necessary to understand and utilize WAF-GUARD.

## Overview

Configuring and maintaining a WAF, especially in large-scale environments, presents significant challenges due to the complexity and interconnectivity of rules and directives. WAF-GUARD addresses these challenges by providing a comprehensive ontology that helps administrators navigate and manage complex WAF configurations efficiently.

## Features

- **Ontology-Based Representation**: Leverages ontology to model WAF configurations, enabling better traceability, transparency, and management.
- **Easy Navigation**: Facilitates the exploration of large rule sets, macros, and configurations through an intuitive interface.
- **Real-World Application**: Tested on extensive real-world configurations, demonstrating its effectiveness in diagnosing and resolving WAF configuration issues.

## Getting Started

### Installation
#### Prerequisites

- **Java 17+**: WAF-GUARD is built using Java, and has been tested with Java 17.
- **Maven**: is used to build the java project. It has been tested with [maven](https://maven.apache.org/) 3.9.6

#### Build from source
1. Clone the repository:
   ```bash
   git clone https://github.com/csvl/CWAF_config.git
   cd CWAF_config/cwaf-ontology
   ```

2. Build the project using Maven:
   ```bash
   mvn clean package
   ```
### Usage

For ontology editing and visualization, we strongly advice to use [Protege](https://protege.stanford.edu/).

Run the different classes:
   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Parser conf/httpd.conf
   ```
   Will produce `config.ttl` and `full_schema.ttl`. The `full_schema.ttl` is designed to be explored manually, while `config.ttl` is provided for the compiler.

   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Compiler 
   ```
   Will produce `entities.ttl` and `full_entities.ttl`. The `full_entities.ttl` is designed to be explored manually, while `entities.ttl` can be imported into the `full_schema.ttl` for a complete overview of the configuration.
 
   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Filter <host> <location> [-p <specific port>] > output.txt
   ```
   Will output the directives information in the same order Apache would applie them. Those directives are filtered based on the host, location, and optionally the port. If you don't want to filter the directives, you can pass the `-a` or `--all` option.

#### Queries

To run SPARQL queries onto an ontology, you can use the [Querier class](https://github.com/csvl/CWAF_config/blob/develop/cwaf-ontology/src/main/java/be/uclouvain/service/Querier.java) by giving it the path to the Ontology model (in *.owl* or *.ttl*) and the query (a text file) as arguments.

   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Querier full_schema.ttl sparql_query_example.txt
   ```

To be used in a scripts, the query can also be passed as a string trough the `-q` argument.

   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Querier full_schema.ttl -q "SELECT ?x WHERE {?x  <http://visualdataweb.org/ontCWAF/includedIn>  <http://visualdataweb.org/ontCWAF/a/file/path>}"
   ```


## License

WAF-GUARD is released under the [MIT License](LICENSE).

## Acknowledgements

This project is supported by Approach Cyber.
This study has been conducted as part of the COODEVIIS project (agreement no. 8887), funded by the Wallonia Public Service (SPW) under the framework of the region’s recovery plan. It was in part supported by the CyberExcellence project (RW, Convention 2110186).

## Contact

For questions, suggestions, or issues, please open an issue on this repository or contact us directly at [bastien.wiaux@uclouvain.be](mailto:bastien.wiaux@uclouvain.be).

---

Thank you for using WAF-GUARD! We hope it enhances your experience in managing WAF configurations.
