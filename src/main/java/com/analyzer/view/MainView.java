// --- MainView.java ---
package com.analyzer.view;

import com.analyzer.controller.AnalysisController;
import com.analyzer.util.FileManager;
import com.analyzer.view.components.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainView {

    private Stage primaryStage;
    private AnalysisController analysisController;

    private BorderPane root;
    private CodeEditorPane codeEditorPane;
    private ResultsTabPane resultsTabPane;
    private Button analyzeButton;
    private Button loadFileButton;
    private Button clearButton;
    private Button saveButton;
    private ProgressIndicator progressIndicator;

    public MainView(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.analysisController = new AnalysisController();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupStyles();
    }

    private void initializeComponents() {
        root = new BorderPane();
        codeEditorPane = new CodeEditorPane();
        resultsTabPane = new ResultsTabPane();

        analyzeButton = new Button("Analizar Código");
        analyzeButton.getStyleClass().addAll("button", "primary-button");

        loadFileButton = new Button("Cargar Archivo");
        loadFileButton.getStyleClass().add("button");

        clearButton = new Button("Limpiar");
        clearButton.getStyleClass().add("button");

        saveButton = new Button("Guardar");
        saveButton.getStyleClass().add("button");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(30, 30);
    }

    private void setupLayout() {
        // Panel superior con botones
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        topPanel.getChildren().addAll(
                loadFileButton, saveButton, new Separator(),
                analyzeButton, clearButton, progressIndicator
        );
        topPanel.getStyleClass().add("top-panel");

        // Panel principal dividido
        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setDividerPositions(0.5);

        // Panel izquierdo (editor)
        VBox leftPanel = new VBox();
        leftPanel.getChildren().add(codeEditorPane);
        leftPanel.getStyleClass().add("left-panel");

        // Panel derecho (resultados)
        VBox rightPanel = new VBox();
        rightPanel.getChildren().add(resultsTabPane);
        rightPanel.getStyleClass().add("right-panel");

        mainSplitPane.getItems().addAll(leftPanel, rightPanel);

        // Configurar layout principal
        root.setTop(topPanel);
        root.setCenter(mainSplitPane);

        // Hacer que los paneles crezcan
        VBox.setVgrow(codeEditorPane, Priority.ALWAYS);
        VBox.setVgrow(resultsTabPane, Priority.ALWAYS);
    }

    private void setupEventHandlers() {
        // Botón Analizar
        analyzeButton.setOnAction(e -> performAnalysis());

        // Botón Cargar Archivo
        loadFileButton.setOnAction(e -> loadFile());

        // Botón Limpiar
        clearButton.setOnAction(e -> clearAll());

        // Botón Guardar
        saveButton.setOnAction(e -> saveFile());

        // Atajo de teclado para analizar (Ctrl+Enter)
        codeEditorPane.getTextArea().setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode().toString().equals("ENTER")) {
                performAnalysis();
            }
        });
    }

    private void performAnalysis() {
        String code = codeEditorPane.getCode();
        if (code.trim().isEmpty()) {
            showAlert("Advertencia", "Por favor ingrese código para analizar.");
            return;
        }

        // Mostrar indicador de progreso
        progressIndicator.setVisible(true);
        analyzeButton.setDisable(true);
        codeEditorPane.setStatus("Analizando...");

        // Ejecutar análisis en hilo separado
        Task<AnalysisController.AnalysisResult> analysisTask = new Task<>() {
            @Override
            protected AnalysisController.AnalysisResult call() {
                return analysisController.performCompleteAnalysis(code);
            }
        };

        analysisTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                AnalysisController.AnalysisResult result = analysisTask.getValue();
                resultsTabPane.updateResults(result);

                progressIndicator.setVisible(false);
                analyzeButton.setDisable(false);

                if (result.isSuccess()) {
                    codeEditorPane.setStatus("Análisis completado");
                    showInfo("Análisis completado exitosamente");
                } else {
                    codeEditorPane.setStatus("Análisis completado con errores");
                }
            });
        });

        analysisTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                analyzeButton.setDisable(false);
                codeEditorPane.setStatus("Error en el análisis");
                showAlert("Error", "Error durante el análisis: " + analysisTask.getException().getMessage());
            });
        });

        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    private void loadFile() {
        String content = FileManager.loadTextFile(primaryStage);
        if (content != null) {
            codeEditorPane.setCode(content);
            codeEditorPane.setStatus("Archivo cargado");
        }
    }

    private void saveFile() {
        String code = codeEditorPane.getCode();
        if (code.trim().isEmpty()) {
            showAlert("Advertencia", "No hay contenido para guardar.");
            return;
        }

        boolean saved = FileManager.saveTextFile(code, primaryStage);
        if (saved) {
            codeEditorPane.setStatus("Archivo guardado");
            showInfo("Archivo guardado exitosamente");
        } else {
            showAlert("Error", "No se pudo guardar el archivo.");
        }
    }

    private void clearAll() {
        codeEditorPane.clearCode();
        resultsTabPane.clearResults();
        codeEditorPane.setStatus("Listo para analizar");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void setupStyles() {
        root.getStyleClass().add("main-view");
    }

    public Scene createScene() {
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
        return scene;
    }
}