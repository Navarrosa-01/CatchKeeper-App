package com.example.it3c_grp10_navarrosa;

public class CatchRecord {
    public enum EntryType {
        SALE, CATCH, NOTE
    }

    public EntryType type;
    public String title;
    public String description;

    // Keep old fields for data that fits this structure
    public String fishType;
    public int quantity;
    public double amountSold;
    public String notes;
    public String date;

    // Old constructor
    public CatchRecord(String fishType, int quantity, double amountSold, String notes, String date) {
        this.fishType = fishType;
        this.quantity = quantity;
        this.amountSold = amountSold;
        this.notes = notes;
        this.date = date;
    }

    // New constructor for the log-style entries
    public CatchRecord(EntryType type, String title, String description, String date) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.date = date;
    }
} 