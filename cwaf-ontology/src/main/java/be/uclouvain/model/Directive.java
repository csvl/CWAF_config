package be.uclouvain.model;

import static be.uclouvain.utils.DirectiveFactory.parseArguments;
import static be.uclouvain.utils.OntUtils.getURIForName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.RDFNode;

import be.uclouvain.service.Constants;
import be.uclouvain.service.context.CompileContext;
import be.uclouvain.vocabulary.OntCWAF;
import org.apache.jena.vocabulary.RDF;


public class Directive implements Comparable<Directive>, Serializable {

    private static Map<Integer, List<Directive>> idsMap = new HashMap<>();
    private static Map<String, List<Directive>> tagsMap = new HashMap<>();

    private int lineNum;
    private transient Individual resource;
    private String resourceURI;
    private String location;
    private String virtualHost;
    private int ifLevel = 0;
    private int phase = Constants.DEFAULT_PHASE; 
    private Integer id;
    private Set<String> tags = new HashSet<>();
    private String[] args;
    private Condition existance_condition;
    private String name;
    private Stack<String> trace;
    private String disabledBy;
    private String type;
    private String fileURI;
    private String entityURI;

    public Directive(CompileContext ctx, Individual resource) {

        this.lineNum = resource.getPropertyValue(OntCWAF.DIR_LINE_NUM).asLiteral().getInt();

        this.name = resource.getLocalName();

        this.location = ctx.getCurrentLocation();
        this.virtualHost = ctx.getCurrentVirtualHost();

        if (resource.hasProperty(OntCWAF.DIR_TYPE)) {
            this.type = resource.getPropertyValue(OntCWAF.DIR_TYPE).asLiteral().getString();
        }

        // Retrieve the directive's file
        ctx.getModel().listStatements(null, OntCWAF.INDIRECT_CONTAINS_DIRECTIVE, resource).forEachRemaining( stmt -> {
            if (stmt.getSubject().hasProperty(OntCWAF.FILE_PATH)) {
                this.fileURI = stmt.getSubject().getURI();
            }
        });

        boolean isSecRule = resource.hasOntClass(OntCWAF.MOD_SEC_RULE);
//        boolean isSecRule = resource.hasOntClass(OntCWAF.MOD_SEC_RULE, true);
//        boolean isSecRule = ctx.getModel().listStatements(resource, RDF.type, OntCWAF.MOD_SEC_RULE).hasNext();
//        boolean isSecRule = resource.hasProperty(RDF.type, OntCWAF.MOD_SEC_RULE);
//        boolean isSecRule = resource.getRDFType() == OntCWAF.MOD_SEC_RULE;
//
        if (isSecRule) {
            if (resource.hasProperty(OntCWAF.RULE_ID)) {
                setId(resource.getPropertyValue(OntCWAF.RULE_ID).asLiteral().getInt());
            }
            if (resource.hasProperty(OntCWAF.RULE_TAG)) {
                resource.listProperties(OntCWAF.RULE_TAG).forEachRemaining( stmt -> {
                    addTag(stmt.getObject().asLiteral().getString());
                });
            }
            if (resource.hasProperty(OntCWAF.PHASE)) {
                this.phase = resource.getPropertyValue(OntCWAF.PHASE).asLiteral().getInt();
            }
        }

        
        RDFNode argNode = resource.getPropertyValue(OntCWAF.ARGUMENTS);
        String argsString = argNode != null ? argNode.asLiteral().getString() : "";
        this.args = parseArguments(argsString, null);

        this.existance_condition = ctx.getEC();

        ctx.getTrace().forEach( ancestor -> {
            if (ancestor.hasOntClass(OntCWAF.IF_FAMILY) || ancestor.hasOntClass(OntCWAF.ELSE_FAMILY)) {
                ifLevel++;
            }
        });

        this.trace = ctx.getCallTrace();

        this.resource = resource;
        this.resourceURI = resource.getURI();
        this.entityURI = getURIForName(name);
    }

    public static void removeById(int id, String disabledBy) {
        if (idsMap.containsKey(id)) {
            idsMap.get(id).forEach( dir -> {
                dir.disabledBy = disabledBy;
            });
        }
    }

    public static void removeByTag(String tag, String disabledBy) {
        Pattern pattern = Pattern.compile(tag);
        tagsMap.keySet().forEach( key -> {
            Matcher matcher = pattern.matcher(key);
            if (matcher.find()) {
                tagsMap.get(key).forEach( dir -> {
                    dir.disabledBy = disabledBy;
                });
            }
        });
    }

    public String getEntityURI() {
        return entityURI;
    }

    public boolean isBeacon() {
        return resource.hasOntClass(OntCWAF.BEACON);
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public void setArgs(String arg, int i) {
        this.args[i] = arg;
    }

    public int getPhase() {
        return phase;
    }

    public int getLineNum() {
        return lineNum;
    }

    public Location getLocation() {
        return new Location(location);
    }

    public VirtualHost getVirtualHost() {
        return new VirtualHost(virtualHost);
    }

    public Individual getIndividual() {
        return resource;
    }

    public String getType() {
        return type;
    }

    public String getFileURI() {
        return fileURI;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Directive other = (Directive) obj;
        return phase == other.phase &&
                ifLevel == other.ifLevel &&
                lineNum == other.lineNum &&
                (virtualHost != null ? virtualHost.equals(other.virtualHost) : other.virtualHost == null) &&
                (location != null ? location.equals(other.location) : other.location == null);
    }

    @Override
    // WARNING: The comparison only make sense if the directives are from the same file
    public int compareTo(Directive other) {
        if (this.phase != other.phase) return Integer.compare(this.phase, other.phase);
        if (this.ifLevel != other.ifLevel) return Integer.compare(this.ifLevel, other.ifLevel);
        if (this.location != null && other.location == null) return 1;
        if (this.location == null && other.location != null) return -1;
        if (this.virtualHost != null && other.virtualHost == null) return 1;
        if (this.virtualHost == null && other.virtualHost != null) return -1;
        return Integer.compare(this.lineNum, other.lineNum);
    }

    @Override
    public String toString() {
        String[] formatted = Arrays.stream(args).map( arg -> {
            if (arg.contains(" ")) {
                arg = "\"" + arg + "\"";
            }
            return arg;
        }).toArray(String[]::new);
        String argsString = String.join(" ", formatted);
        String EC = existance_condition.getCondition();
        return (disabledBy == null ? "" : "X ") + "{" +
                "phase=" + phase +
                ", ifLevel=" + ifLevel +
                ", location='" + (location == null ? "global" : location) + '\'' +
                ", virtualHost='" + (virtualHost== null ? "" : virtualHost) + '\'' +
                ", id=" + id +
                ", tags=" + tags +
                ", in " + fileURI + ":" + lineNum +
                (EC == "true" ? "" : ", EC=" + EC) +
                "}\t\t" + name + "\t\t(" + argsString + ")" + (disabledBy == null ? "" : " disabled by " + disabledBy);
    }

    public void setPhase(int newPhase) {
        this.phase = newPhase;
    }

    public void setId(int id) {
        this.id = id;
        if (idsMap.containsKey(id)) {
            idsMap.get(id).add(this);
        } else {
            idsMap.put(id, new ArrayList<>(List.of(this)));
        }
    }

    public void replaceTag(String oldTag, String newTag) {
        if (tags.contains(oldTag)) {
            tags.remove(oldTag);
            tags.add(newTag);
            tagsMap.get(oldTag).remove(this);
            if (tagsMap.containsKey(newTag)) {
                tagsMap.get(newTag).add(this);
            } else {
                tagsMap.put(newTag, new ArrayList<>(List.of(this)));
            }
        } else {
            System.err.println("Tag " + oldTag + " not found in directive " + name + "; cannot replace it by " + newTag);
        }
    }

    public void addTag(String tag) {
        tags.add(tag);
        if (tagsMap.containsKey(tag)) {
            tagsMap.get(tag).add(this);
        } else {
            tagsMap.put(tag, new ArrayList<>(List.of(this)));
        }
    }

    public void updateContext(CompileContext ctx) {
        this.location = ctx.getCurrentLocation();
        this.virtualHost = ctx.getCurrentVirtualHost();
    }

    public Individual toEntityIndividual(OntModel model) {
        Individual ind = model.createIndividual(entityURI, resource.getOntClass());
        ind.addLiteral(OntCWAF.DIR_LINE_NUM, lineNum);
        ind.addLiteral(OntCWAF.PHASE, phase);
        if (id != null) {
            ind.addLiteral(OntCWAF.RULE_ID, id);
        }
        if (!tags.isEmpty()) {
            tags.forEach( tag -> ind.addLiteral(OntCWAF.RULE_TAG, tag));
        }
        if (location != null) {
            ind.addLiteral(OntCWAF.LOCATION_PATH, location);
        }
        if (virtualHost != null) {
            ind.addLiteral(OntCWAF.VIRTUAL_HOST_NAME, virtualHost);
        }
        ind.addLiteral(OntCWAF.ARGUMENTS, String.join(" ", args));
        ind.addProperty(OntCWAF.INSTANCE_OF, resource);
        ind.addLiteral(OntCWAF.STACK_TRACE, trace.toString());
        ind.addProperty(OntCWAF.CONTAINED_IN, fileURI);
        return ind; 
    }
}
