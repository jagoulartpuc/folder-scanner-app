module com.example.folderscannerapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.folderscannerapp to javafx.fxml;
    exports com.example.folderscannerapp;
}