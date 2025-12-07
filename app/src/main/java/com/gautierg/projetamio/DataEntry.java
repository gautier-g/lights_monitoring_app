package com.gautierg.projetamio;

public class DataEntry implements Comparable<DataEntry> {
    public long timestamp;
    public boolean isPoweredOn;
    public double value;
    public String mote;

    @Override
    public int compareTo(DataEntry other) {
        return Long.compare(this.timestamp, other.timestamp);
    }
}
