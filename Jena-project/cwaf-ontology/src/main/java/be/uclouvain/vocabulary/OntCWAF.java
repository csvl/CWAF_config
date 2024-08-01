package be.uclouvain.vocabulary;

/* CVS $Id: $ */
 
import org.apache.jena.rdf.model.*;
import org.apache.jena.ontology.*;
 
/**
 * Vocabulary definitions from ontCWAF_1_0.owl 
 * @author Auto-generated by schemagen on 31 juil. 2024 15:13 
 */
public class OntCWAF {
    /** <p>The ontology model that holds the vocabulary terms</p> */
    private static final OntModel M_MODEL = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://visualdataweb.org/ontCWAF/";
    
    /** <p>The namespace of the vocabulary as a string</p>
     * @return namespace as String
     * @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = M_MODEL.createResource( NS );
    
    /** <p>The ontology's owl:versionInfo as a string</p> */
    public static final String VERSION_INFO = "1.0";
    
    public static final ObjectProperty CONTAINED_IN = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/containedIn" );
    
    public static final ObjectProperty CONTAINS_DIRECTIVE = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/containsDirective" );
    
    public static final ObjectProperty CONTAINS_FILE = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/containsFile" );
    
    public static final ObjectProperty IF_CHAIN = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/ifChain" );
    
    public static final ObjectProperty INCLUDE_FILE = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/includeFile" );
    
    public static final ObjectProperty INSTANCE_OF = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/instanceOf" );
    
    public static final ObjectProperty IS_ENDING_LOCATION = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/isEndingLocation" );
    
    public static final ObjectProperty IS_ENDING_VIRTUAL_HOST = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/isEndingVirtualHost" );
    
    public static final ObjectProperty USE_MACRO = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/useMacro" );
    
    public static final ObjectProperty USED_BY = M_MODEL.createObjectProperty( "http://visualdataweb.org/ontCWAF/usedBy" );
    
    public static final DatatypeProperty ARGUMENTS = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/arguments" );
    
    public static final DatatypeProperty CONDITION = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/condition" );
    
    public static final DatatypeProperty CONFIG_NAME = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/configName" );
    
    public static final DatatypeProperty DIR_LINE_NUM = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/dirLineNum" );
    
    public static final DatatypeProperty DIR_TYPE = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/dirType" );
    
    public static final DatatypeProperty FILE_PATH = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/filePath" );
    
    public static final DatatypeProperty IF_TYPE = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/ifType" );
    
    public static final DatatypeProperty IS_PLACE_HOLDER = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/isPlaceHolder" );
    
    public static final DatatypeProperty LOCATION_PATH = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/locationPath" );
    
    public static final DatatypeProperty MACRO_NAME = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/macroName" );
    
    public static final DatatypeProperty MACRO_PARAMS = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/macroParams" );
    
    public static final DatatypeProperty PHASE = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/phase" );
    
    public static final DatatypeProperty RULE_ID = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/ruleId" );
    
    public static final DatatypeProperty RULE_TAG = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/ruleTag" );
    
    public static final DatatypeProperty SERVER_NAME = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/serverName" );
    
    public static final DatatypeProperty STACK_TRACE = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/stackTrace" );
    
    public static final DatatypeProperty USE_ARGS = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/useArgs" );
    
    public static final DatatypeProperty VIRTUAL_HOST_NAME = M_MODEL.createDatatypeProperty( "http://visualdataweb.org/ontCWAF/virtualHostName" );
    
    public static final OntClass BEACON = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Beacon" );
    
    public static final OntClass CONFIGURATION = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Configuration" );
    
    public static final OntClass DIRECTIVE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Directive" );
    
    public static final OntClass ELSE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Else" );
    
    public static final OntClass ELSE_FAMILY = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/ElseFamily" );
    
    public static final OntClass ELSE_IF = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/ElseIf" );
    
    public static final OntClass END_LOCATION = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/EndLocation" );
    
    public static final OntClass END_VIRTUAL_HOST = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/EndVirtualHost" );
    
    public static final OntClass FILE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/File" );
    
    public static final OntClass IF = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/If" );
    
    public static final OntClass IF_FAMILY = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/IfFamily" );
    
    public static final OntClass IF_RULE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/IfRule" );
    
    public static final OntClass INCLUDE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Include" );
    
    public static final OntClass LOCATION = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Location" );
    
    public static final OntClass MACRO = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Macro" );
    
    public static final OntClass MOD_SEC_RULE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/ModSecRule" );
    
    public static final OntClass RULE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Rule" );
    
    public static final OntClass SCOPE_RULE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/ScopeRule" );
    
    public static final OntClass USE = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/Use" );
    
    public static final OntClass VIRTUAL_HOST = M_MODEL.createClass( "http://visualdataweb.org/ontCWAF/VirtualHost" );
    
}
