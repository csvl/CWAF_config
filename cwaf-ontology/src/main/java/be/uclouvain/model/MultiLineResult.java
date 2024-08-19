package be.uclouvain.model;

public class MultiLineResult {

    private String multiline;
    private int linesRead;

    public MultiLineResult(String multiline, int linesRead) {
        this.multiline = multiline;
        this.linesRead = linesRead;
    }

    public String getMultiline() {
        return multiline;
    }

    public int getLinesRead() {
        return linesRead;
    }
}