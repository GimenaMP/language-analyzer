// --- ResultsTabPane.java ---
package com.analyzer.view.components;

import com.analyzer.model.*;
import com.analyzer.controller.AnalysisController;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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

    @SuppressWarnings("unchecked")
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

        // Tabla de Errores
        TableColumn<AnalysisError, String> errorTypeCol = new TableColumn<>("Tipo");
        errorTypeCol.setCellValueFactory(new PropertyValueFactory<>("errorType"));
        errorTypeCol.setPrefWidth(100);

        TableColumn<AnalysisError, String> errorMessageCol = new TableColumn<>("Mensaje");
        errorMessageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        errorMessageCol.setPrefWidth(300);

        TableColumn<AnalysisError, String> errorPositionCol = new TableColumn<>("Posición");
        errorPositionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        errorPositionCol.setPrefWidth(80);

        errorsTable.getColumns().addAll(errorTypeCol, errorMessageCol, errorPositionCol);

        // Tabla de Símbolos
        TableColumn<Symbol, String> symbolNameCol = new TableColumn<>("Nombre");
        symbolNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbolNameCol.setPrefWidth(120);
//
        // Asegúrate de que Symbol tenga un método getName() que retorne el nombre del símbolo
        TableColumn<Symbol, String> symbolTypeCol = new TableColumn<>("Tipo");
        symbolTypeCol.setCellValueFactory(new PropertyValueFactory<>("symbolType"));
        symbolTypeCol.setPrefWidth(100);
        // Asegúrate de que Symbol tenga un método getSymbolType() que retorne el tipo del símbolo

        TableColumn<Symbol, String> symbolDataTypeCol = new TableColumn<>("Tipo Dato");
        symbolDataTypeCol.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        symbolDataTypeCol.setPrefWidth(100);
        // Asegúrate de que Symbol tenga un método getDataType() que retorne el tipo de dato del símbolo

        TableColumn<Symbol, String> symbolScopeCol = new TableColumn<>("Ámbito");
        symbolScopeCol.setCellValueFactory(new PropertyValueFactory<>("scope"));
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
        // Actualizar lenguaje
        if (result.getLanguage() != null) {
           languageLabel.setText(result.getLanguage().getDisplayName() +
            " - " + result.getLanguage().getDescription());
        }

        // Actualizar tokens
        if (result.getTokens() != null) {
            ObservableList<Token> tokenData = FXCollections.observableArrayList(result.getTokens());
            tokensTable.setItems(tokenData);
        }

        // Actualizar errores
        List<AnalysisError> allErrors = result.getAllErrors();
        ObservableList<AnalysisError> errorData = FXCollections.observableArrayList(allErrors);
        errorsTable.setItems(errorData);

        // Actualizar símbolos
        if (result.getSymbolTable() != null) {
            ObservableList<Symbol> symbolData = FXCollections.observableArrayList(
                    result.getSymbolTable().values()
            );
            symbolsTable.setItems(symbolData);
        }

        // Actualizar salida
        if (result.getExecutionOutput() != null) {
            String output = String.join("\n", result.getExecutionOutput());
            outputTextArea.setText(output);
        }

        // Actualizar badges de conteo en las pestañas
        tokensTab.setText("Tokens (" + (result.getTokens() != null ? result.getTokens().size() : 0) + ")");
        errorsTab.setText("Errores (" + allErrors.size() + ")");
        symbolsTab.setText("Símbolos (" + (result.getSymbolTable() != null ? result.getSymbolTable().size() : 0) + ")");
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