
// --- FileManager.java ---
package com.analyzer.util;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileManager {

    /**
     * Abre un diálogo para seleccionar y cargar un archivo de texto
     */
    public static String loadTextFile(Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar archivo de código");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"),
                new FileChooser.ExtensionFilter("Archivos HTML", "*.html", "*.htm"),
                new FileChooser.ExtensionFilter("Archivos Python", "*.py"),
                new FileChooser.ExtensionFilter("Archivos SQL", "*.sql"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(parentStage);
        if (selectedFile != null) {
            try {
                return Files.readString(Paths.get(selectedFile.getAbsolutePath()));
            } catch (IOException e) {
                System.err.println("Error al cargar el archivo: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Guarda texto en un archivo
     */
    public static boolean saveTextFile(String content, Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar archivo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo de texto", "*.txt")
        );

        File file = fileChooser.showSaveDialog(parentStage);
        if (file != null) {
            try {
                Files.writeString(Paths.get(file.getAbsolutePath()), content);
                return true;
            } catch (IOException e) {
                System.err.println("Error al guardar el archivo: " + e.getMessage());
                return false;
            }
        }
        return false;
    }
}
