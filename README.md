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

### Prerequisites

- **Java 8+**: WAF-GUARD is built using Java, so ensure you have Java 8 or higher installed.
- **Protege**: For ontology editing and visualization, you will need to install [Protege](https://protege.stanford.edu/).

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/csvl/CWAF_config.git
   cd CWAF_config/cwaf-ontology
   ```

2. Build the project using Maven:
   ```bash
   mvn clean package
   ```
3. Run the different classes:
   ```bash
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Parser conf/httpd.conf
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Compiler 
   java -cp cwaf-ontology/target/cwaf-ontology-1.0-SNAPSHOT.jar be.uclouvain.service.Filter > output
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
