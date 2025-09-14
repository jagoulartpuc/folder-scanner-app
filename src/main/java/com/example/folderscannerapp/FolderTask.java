package com.example.folderscannerapp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

class FolderTask extends RecursiveTask<List<FolderInfo>> {
    private final File folder;

    FolderTask(File folder) {
        this.folder = folder;
    }

    @Override
    protected List<FolderInfo> compute() {
        List<FolderInfo> results = new ArrayList<>();
        List<FolderTask> subTasks = new ArrayList<>();
        long directSize = 0;

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    directSize += file.length();
                } else if (file.isDirectory()) {
                    FolderTask task = new FolderTask(file);
                    subTasks.add(task);
                    task.fork();
                }
            }
        }

        long subfolderTotalSize = 0;
        for (FolderTask task : subTasks) {
            List<FolderInfo> taskResults = task.join();
            results.addAll(taskResults);

            for (FolderInfo info : taskResults) {
                if (info.getPath().equals(task.folder.getAbsolutePath())) {
                    subfolderTotalSize += (long) info.getSizeBytes();
                    break;
                }
            }
        }

        double totalSize = (directSize + subfolderTotalSize) / (1024.0 * 1024.0 * 1024.0);
        results.add(new FolderInfo(folder.getAbsolutePath(), totalSize));

        return results;
    }
}
