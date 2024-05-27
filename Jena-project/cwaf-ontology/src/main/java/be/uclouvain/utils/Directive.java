package be.uclouvain.utils;

public class Directive implements Comparable<Directive> {
    private String location;
    private String virtualHost;
    private int ifLevel;
    private String macroName;
    private String macroCallerInstr;
    private int instrNumber;
    private String name;

    public Directive(String location, String virtualHost, int ifLevel, String macroName, String macroCallerInstr, int instrNumber, String name) {
        this.location = location;
        this.virtualHost = virtualHost;
        this.ifLevel = ifLevel;
        this.macroName = macroName;
        this.macroCallerInstr = macroCallerInstr;
        this.instrNumber = instrNumber;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Directive other = (Directive) obj;
        return ifLevel == other.ifLevel &&
                instrNumber == other.instrNumber &&
                (virtualHost != null ? virtualHost.equals(other.virtualHost) : other.virtualHost == null) &&
                (location != null ? location.equals(other.location) : other.location == null);
    }

    @Override
    public int compareTo(Directive other) {
        if (this.ifLevel != other.ifLevel) return Integer.compare(this.ifLevel, other.ifLevel);
        if (this.location != null && other.location == null) return 1;
        if (this.location == null && other.location != null) return -1;
        if (this.virtualHost != null && other.virtualHost == null) return 1;
        if (this.virtualHost == null && other.virtualHost != null) return -1;
        return Integer.compare(this.instrNumber, other.instrNumber);
    }

    // Getters and Setters can be added as needed
}
