package com.example.folderscannerapp;

import java.util.List;

public class AnalysisResult {
    final List<FolderInfo> results;
    final double executionTime;

    public AnalysisResult(List<FolderInfo> results, double executionTime) {
        this.results = results;
        this.executionTime = executionTime;
    }


}
