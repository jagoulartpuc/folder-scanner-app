package com.example.folderscannerapp;

public class FolderInfo {
    private final String path;
    private final double sizeBytes;
    private final String formattedSize;

    public FolderInfo(String path, double sizeBytes) {
        this.path = path;
        this.sizeBytes = sizeBytes;
        this.formattedSize = String.format("%.2f", sizeBytes);
    }

    public String getPath() {
        return path;
    }

    public double getSizeBytes() {
        return sizeBytes;
    }

    public String getFormattedSize() {
        return formattedSize;
    }

    @Override
    public String toString() {
        return String.format("Folder: %s | Size: %.2f GB", path, sizeBytes);
    }
}
