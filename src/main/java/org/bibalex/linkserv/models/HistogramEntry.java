package org.bibalex.linkserv.models;

public class HistogramEntry {
    private int key;
    private int count;

    public HistogramEntry(int key, int count) {
        this.key = key;
        this.count = count;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
