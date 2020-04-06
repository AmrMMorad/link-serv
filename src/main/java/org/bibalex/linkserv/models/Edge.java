package org.bibalex.linkserv.models;

public class Edge {

    private String id;
    private String type;
    private String source;
    private String target;

    public Edge(String id, String type, String source, String target) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
