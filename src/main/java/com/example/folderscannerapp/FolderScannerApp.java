package com.example.folderscannerapp;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class FolderScannerApp extends Application {

    private static final ForkJoinPool pool = ForkJoinPool.commonPool();

    private TextField pathTextField;
    private Button analyzeButton;
    private Button browseButton;
    private TableView<FolderInfo> resultsTable;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private ObservableList<FolderInfo> tableData;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Folder Scanner App");

        initializeComponents();

        VBox root = createLayout();

        setupTable();

        setupEventHandlers(primaryStage);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initializeComponents() {
        pathTextField = new TextField();
        pathTextField.setPromptText("Enter folder path (e.g., C:\\Users)");
        pathTextField.setPrefWidth(400);

        analyzeButton = new Button("Analyze");
        analyzeButton.setPrefWidth(80);

        browseButton = new Button("Browse...");
        browseButton.setPrefWidth(80);

        resultsTable = new TableView<>();
        resultsTable.setPrefHeight(400);

        statusLabel = new Label("Ready to analyze");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);

        tableData = FXCollections.observableArrayList();
        resultsTable.setItems(tableData);
    }

    private VBox createLayout() {
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getChildren().addAll(
                new Label("Folder Path:"),
                pathTextField,
                browseButton,
                analyzeButton
        );

        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.getChildren().addAll(progressIndicator, statusLabel);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(
                inputBox,
                statusBox,
                new Label("Top 20 Largest Folders:"),
                resultsTable
        );

        return root;
    }

    private void setupTable() {
        TableColumn<FolderInfo, String> pathColumn = new TableColumn<>("Folder Path");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathColumn.setPrefWidth(600);
        pathColumn.setResizable(true);

        TableColumn<FolderInfo, String> sizeColumn = new TableColumn<>("Size (GB)");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
        sizeColumn.setPrefWidth(120);
        sizeColumn.setResizable(true);

        ObservableList<TableColumn<FolderInfo, ?>> columns = resultsTable.getColumns();
        columns.add(pathColumn);
        columns.add(sizeColumn);

        resultsTable.setPlaceholder(new Label("No data to display. Click 'Analyze' to start."));
    }

    private void setupEventHandlers(Stage primaryStage) {
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Folder to Analyze");

            String currentPath = pathTextField.getText();
            if (!currentPath.isEmpty()) {
                File initialDir = new File(currentPath);
                if (initialDir.exists() && initialDir.isDirectory()) {
                    directoryChooser.setInitialDirectory(initialDir);
                }
            }

            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                pathTextField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        analyzeButton.setOnAction(e -> analyzeFolder());

        pathTextField.setOnAction(e -> analyzeFolder());
    }

    private void analyzeFolder() {
        String basePath = pathTextField.getText().trim();

        if (basePath.isEmpty()) {
            showAlert("Please enter a folder path or use the Browse button to select a folder.");
            return;
        }

        File baseDir = new File(basePath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            showAlert("Invalid directory: " + basePath + "\nPlease check the path and try again.");
            return;
        }

        tableData.clear();

        progressIndicator.setVisible(true);
        statusLabel.setText("Analyzing folders...");
        analyzeButton.setDisable(true);
        browseButton.setDisable(true);
        pathTextField.setDisable(true);

        Task<AnalysisResult> analysisTask = new Task<>() {
            @Override
            protected AnalysisResult call() {
                long startTime = System.nanoTime();

                FolderTask rootTask = new FolderTask(baseDir);
                List<FolderInfo> results = pool.invoke(rootTask);

                long endTime = System.nanoTime();
                double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;

                removeDuplicates(results);
                results.sort(Comparator.comparing(FolderInfo::getSizeBytes).reversed());
                if (results.size() > 20) {
                    results = results.subList(0, 20);
                }

                return new AnalysisResult(results, durationInSeconds);
            }

            @Override
            protected void succeeded() {
                AnalysisResult results = getValue();
                tableData.setAll(results.results);
                statusLabel.setText(String.format("Analysis completed in %.2f seconds", results.executionTime));
                progressIndicator.setVisible(false);
                analyzeButton.setDisable(false);
                browseButton.setDisable(false);
                pathTextField.setDisable(false);
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                showAlert("Analysis failed: " + exception.getMessage());

                statusLabel.setText("Analysis failed");
                progressIndicator.setVisible(false);
                analyzeButton.setDisable(false);
                browseButton.setDisable(false);
                pathTextField.setDisable(false);
            }
        };

        // Run task in background thread
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Folder Analyzer");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void removeDuplicates(List<FolderInfo> folders) {
        Set<Double> seenSizes = new HashSet<>();
        folders.removeIf(folder -> !seenSizes.add(folder.getSizeBytes()));
    }

    public static void main(String[] args) {
        launch(args);
    }
}