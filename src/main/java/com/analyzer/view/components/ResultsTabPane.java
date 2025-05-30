// --- ResultsTabPane.java ---
package com.analyzer.view.components;

import com.analyzer.model.*;
import com.analyzer.controller.AnalysisController;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
// Agregar estos imports en ResultsTabPane.java
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.Map;

public class ResultsTabPane extends TabPane {

    private Tab languageTab;
    private Tab tokensTab;
    private Tab errorsTab;
    private Tab symbolsTab;
    private Tab outputTab;

    private Label languageLabel;
    private TableView<Token> tokensTable;
    private TableView<AnalysisError> errorsTable;
    private TableView<Symbol> symbolsTable;
    private TextArea outputTextArea;

    public ResultsTabPane() {
        initializeTabs();
        setupTables();
        setupStyles();
    }

    private void initializeTabs() {
        // Tab de Lenguaje Detectado
        languageTab = new Tab("Lenguaje");
        languageTab.setClosable(false);
        languageLabel = new Label("No detectado");
        languageLabel.getStyleClass().add("language-label");
        VBox languageBox = new VBox(new Label("Lenguaje detectado:"), languageLabel);
        languageBox.setPadding(new Insets(20));
        languageBox.setSpacing(10);
        languageTab.setContent(languageBox);

        // Tab de Tokens
        tokensTab = new Tab("Tokens");
        tokensTab.setClosable(false);
        tokensTable = new TableView<>();
        VBox tokensBox = new VBox(tokensTable);
        tokensBox.setPadding(new Insets(10));
        tokensTab.setContent(tokensBox);

        // Tab de Errores
        errorsTab = new Tab("Errores");
        errorsTab.setClosable(false);
        errorsTable = new TableView<>();
        VBox errorsBox = new VBox(errorsTable);
        errorsBox.setPadding(new Insets(10));
        errorsTab.setContent(errorsBox);

        // Tab de Símbolos
        symbolsTab = new Tab("Símbolos");
        symbolsTab.setClosable(false);
        symbolsTable = new TableView<>();
        VBox symbolsBox = new VBox(symbolsTable);
        symbolsBox.setPadding(new Insets(10));
        symbolsTab.setContent(symbolsBox);

        // Tab de Salida
        outputTab = new Tab("Salida");
        outputTab.setClosable(false);
        outputTextArea = new TextArea();
        outputTextArea.setEditable(false);
        outputTextArea.getStyleClass().add("output-text-area");
        VBox outputBox = new VBox(outputTextArea);
        outputBox.setPadding(new Insets(10));
        outputTab.setContent(outputBox);
        VBox.setVgrow(outputTextArea, Priority.ALWAYS);

        getTabs().addAll(languageTab, tokensTab, errorsTab, symbolsTab, outputTab);
    }

    @SuppressWarnings({"unchecked", "unchecked"})
    private void setupTables() {
        // Tabla de Tokens
        TableColumn<Token, String> tokenValueCol = new TableColumn<>("Valor");
        tokenValueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        tokenValueCol.setPrefWidth(150);

        TableColumn<Token, String> tokenTypeCol = new TableColumn<>("Tipo");
        tokenTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        tokenTypeCol.setPrefWidth(100);

        TableColumn<Token, String> tokenPositionCol = new TableColumn<>("Posición");
        tokenPositionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        tokenPositionCol.setPrefWidth(80);

        tokensTable.getColumns().addAll(tokenValueCol, tokenTypeCol, tokenPositionCol);

        // Tabla de Errores - CORREGIDO
        TableColumn<AnalysisError, String> errorTypeCol = new TableColumn<>("Tipo");
        errorTypeCol.setCellValueFactory(cellData -> {
            AnalysisError error = cellData.getValue();
            return new SimpleStringProperty(error.getErrorType().getDisplayName());
        });
        errorTypeCol.setPrefWidth(100);
        errorTypeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Aplicar estilo según tipo de error
                    switch (item) {
                        case "Léxico":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                        case "Sintáctico":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "Semántico":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                    }
                }
            }
        });

        TableColumn<AnalysisError, String> errorMessageCol = new TableColumn<>("Mensaje");
        errorMessageCol.setCellValueFactory(cellData -> {
            AnalysisError error = cellData.getValue();
            return new SimpleStringProperty(error.getMessage());
        });
        errorMessageCol.setPrefWidth(300);

        TableColumn<AnalysisError, String> errorPositionCol = new TableColumn<>("Posición");
        errorPositionCol.setCellValueFactory(cellData -> {
            AnalysisError error = cellData.getValue();
            return new SimpleStringProperty(error.getPosition());
        });
        errorPositionCol.setPrefWidth(80);

        errorsTable.getColumns().addAll(errorTypeCol, errorMessageCol, errorPositionCol);

        // Tabla de Símbolos - CORREGIDO
        TableColumn<Symbol, String> symbolNameCol = new TableColumn<>("Nombre");
        symbolNameCol.setCellValueFactory(cellData -> {
            Symbol symbol = cellData.getValue();
            return new SimpleStringProperty(symbol.getName());
        });
        symbolNameCol.setPrefWidth(120);

        TableColumn<Symbol, String> symbolTypeCol = new TableColumn<>("Tipo");
        symbolTypeCol.setCellValueFactory(cellData -> {
            Symbol symbol = cellData.getValue();
            return new SimpleStringProperty(symbol.getSymbolType().getDisplayName());
        });
        symbolTypeCol.setPrefWidth(100);

        TableColumn<Symbol, String> symbolDataTypeCol = new TableColumn<>("Tipo Dato");
        symbolDataTypeCol.setCellValueFactory(cellData -> {
            Symbol symbol = cellData.getValue();
            return new SimpleStringProperty(symbol.getDataType());
        });
        symbolDataTypeCol.setPrefWidth(100);

        TableColumn<Symbol, String> symbolScopeCol = new TableColumn<>("Ámbito");
        symbolScopeCol.setCellValueFactory(cellData -> {
            Symbol symbol = cellData.getValue();
            return new SimpleStringProperty(symbol.getScope());
        });
        symbolScopeCol.setPrefWidth(100);

        symbolsTable.getColumns().addAll(symbolNameCol, symbolTypeCol, symbolDataTypeCol, symbolScopeCol);
    }

    private void setupStyles() {
        getStyleClass().add("results-tab-pane");
        tokensTable.getStyleClass().add("data-table");
        errorsTable.getStyleClass().add("data-table");
        symbolsTable.getStyleClass().add("data-table");
    }

    public void updateResults(AnalysisController.AnalysisResult result) {
        System.out.println("=== UPDATING RESULTS DEBUG ===");

        try {
            // Actualizar lenguaje
            if (result.getLanguage() != null) {
                languageLabel.setText(result.getLanguage().getDisplayName());
                System.out.println("Language: " + result.getLanguage().getDisplayName());
            } else {
                languageLabel.setText("No detectado");
                System.out.println("Language: null");
            }

            // Actualizar tokens
            ObservableList<Token> tokenData = FXCollections.observableArrayList();
            if (result.getTokens() != null && !result.getTokens().isEmpty()) {
                tokenData.addAll(result.getTokens());
                System.out.println("Tokens count: " + tokenData.size());
                // Mostrar algunos tokens de ejemplo
                for (int i = 0; i < Math.min(3, tokenData.size()); i++) {
                    Token token = tokenData.get(i);
                    System.out.println("  Token " + i + ": " + token.getValue() + " (" + token.getType() + ") [" + token.getLine() + ":" + token.getColumn() + "]");
                }
            } else {
                System.out.println("No tokens found");
            }
            tokensTable.setItems(tokenData);

            // Actualizar errores - CON DEBUGGING DETALLADO
            ObservableList<AnalysisError> errorData = FXCollections.observableArrayList();

            System.out.println("=== ERRORS DEBUG ===");

            if (result.getLexicalErrors() != null) {
                System.out.println("Lexical errors count: " + result.getLexicalErrors().size());
                for (AnalysisError error : result.getLexicalErrors()) {
                    System.out.println("  Lexical Error: " + error.getFullMessage());
                    System.out.println("    Type: " + error.getErrorType());
                    System.out.println("    Message: " + error.getMessage());
                    System.out.println("    Position: " + error.getPosition());
                }
                errorData.addAll(result.getLexicalErrors());
            }

            if (result.getSyntacticErrors() != null) {
                System.out.println("Syntactic errors count: " + result.getSyntacticErrors().size());
                for (AnalysisError error : result.getSyntacticErrors()) {
                    System.out.println("  Syntactic Error: " + error.getFullMessage());
                }
                errorData.addAll(result.getSyntacticErrors());
            }

            if (result.getSemanticErrors() != null) {
                System.out.println("Semantic errors count: " + result.getSemanticErrors().size());
                for (AnalysisError error : result.getSemanticErrors()) {
                    System.out.println("  Semantic Error: " + error.getFullMessage());
                }
                errorData.addAll(result.getSemanticErrors());
            }

            System.out.println("Total errors for table: " + errorData.size());

            // Verificar que los errores tienen datos válidos
            for (int i = 0; i < errorData.size(); i++) {
                AnalysisError error = errorData.get(i);
                System.out.println("Error " + i + " validation:");
                System.out.println("  Message null?: " + (error.getMessage() == null));
                System.out.println("  Type null?: " + (error.getErrorType() == null));
                System.out.println("  Position: " + error.getLine() + ":" + error.getColumn());
            }

            errorsTable.setItems(errorData);

            // Forzar refresh de la tabla
            errorsTable.refresh();

            // Actualizar símbolos
            ObservableList<Symbol> symbolData = FXCollections.observableArrayList();
            if (result.getSymbolTable() != null && !result.getSymbolTable().isEmpty()) {
                symbolData.addAll(result.getSymbolTable().values());
                System.out.println("Symbols count: " + symbolData.size());
                // Mostrar algunos símbolos de ejemplo
                for (int i = 0; i < Math.min(3, symbolData.size()); i++) {
                    Symbol symbol = symbolData.get(i);
                    System.out.println("  Symbol " + i + ": " + symbol.getName() + " (" + symbol.getSymbolType() + ")");
                }
            } else {
                System.out.println("No symbols found");
            }
            symbolsTable.setItems(symbolData);

            // Actualizar salida de ejecución
            if (result.getExecutionOutput() != null && !result.getExecutionOutput().isEmpty()) {
                outputTextArea.setText(String.join("\n", result.getExecutionOutput()));
            } else {
                outputTextArea.setText("No hay salida de ejecución disponible.");
            }

            // Actualizar contadores en las pestañas
            int totalErrors = errorData.size();
            int totalTokens = tokenData.size();
            int totalSymbols = symbolData.size();

            errorsTab.setText(String.format("Errores (%d)", totalErrors));
            tokensTab.setText(String.format("Tokens (%d)", totalTokens));
            symbolsTab.setText(String.format("Símbolos (%d)", totalSymbols));

            System.out.println("Tab counters - Errors: " + totalErrors + ", Tokens: " + totalTokens + ", Symbols: " + totalSymbols);

            // Si hay errores, cambiar a la pestaña de errores
            if (totalErrors > 0) {
                getSelectionModel().select(errorsTab);
                System.out.println("Switched to errors tab");
            }

        } catch (Exception e) {
            System.err.println("Error updating results: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== RESULTS UPDATE COMPLETE ===");
    }

    public void clearResults() {
        languageLabel.setText("No detectado");
        tokensTable.getItems().clear();
        errorsTable.getItems().clear();
        symbolsTable.getItems().clear();
        outputTextArea.clear();

        tokensTab.setText("Tokens");
        errorsTab.setText("Errores");
        symbolsTab.setText("Símbolos");
    }
}