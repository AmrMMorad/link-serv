package org.bibalex.linkserv.models;

public class Node {
    private String id;
    private String type;
    private String url;
    private String timestamp;

    public Node(String id, String type, String url, String timestamp) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
