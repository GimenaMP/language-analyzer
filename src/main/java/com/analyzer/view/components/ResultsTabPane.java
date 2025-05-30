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

        // Mejorar la tabla de símbolos
        TableColumn<Symbol, String> symbolNameCol = new TableColumn<>("Nombre");
        symbolNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        symbolNameCol.setPrefWidth(120);

        TableColumn<Symbol, Symbol.SymbolType> symbolTypeCol = new TableColumn<>("Tipo");
        symbolTypeCol.setCellValueFactory(new PropertyValueFactory<>("symbolType"));
        symbolTypeCol.setPrefWidth(100);
        symbolTypeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Symbol.SymbolType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });

        TableColumn<Symbol, String> symbolScopeCol = new TableColumn<>("Ámbito");
        symbolScopeCol.setCellValueFactory(new PropertyValueFactory<>("scope"));
        symbolScopeCol.setPrefWidth(100);

        TableColumn<Symbol, Integer> symbolLineCol = new TableColumn<>("Línea");
        symbolLineCol.setCellValueFactory(new PropertyValueFactory<>("declarationLine"));
        symbolLineCol.setPrefWidth(60);

        TableColumn<Symbol, Integer> symbolColumnCol = new TableColumn<>("Columna");
        symbolColumnCol.setCellValueFactory(new PropertyValueFactory<>("declarationColumn"));
        symbolColumnCol.setPrefWidth(70);

        symbolsTable.getColumns().setAll(symbolNameCol, symbolTypeCol, symbolScopeCol, 
                                       symbolLineCol, symbolColumnCol);

        // Mejorar la tabla de errores
        TableColumn<AnalysisError, AnalysisError.ErrorType> errorTypeCol = 
            new TableColumn<>("Tipo");
        errorTypeCol.setCellValueFactory(new PropertyValueFactory<>("errorType"));
        errorTypeCol.setPrefWidth(100);
        errorTypeCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(AnalysisError.ErrorType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getDisplayName());
                    switch (item) {
                        case LEXICAL:
                            setStyle("-fx-text-fill: red;");
                            break;
                        case SYNTACTIC:
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case SEMANTIC:
                            setStyle("-fx-text-fill: blue;");
                            break;
                    }
                }
            }
        });

        TableColumn<AnalysisError, String> errorMessageCol = new TableColumn<>("Mensaje");
        errorMessageCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        errorMessageCol.setPrefWidth(300);

        TableColumn<AnalysisError, Integer> errorLineCol = new TableColumn<>("Línea");
        errorLineCol.setCellValueFactory(new PropertyValueFactory<>("line"));
        errorLineCol.setPrefWidth(60);

        TableColumn<AnalysisError, Integer> errorColumnCol = new TableColumn<>("Columna");
        errorColumnCol.setCellValueFactory(new PropertyValueFactory<>("column"));
        errorColumnCol.setPrefWidth(70);

        errorsTable.getColumns().setAll(errorTypeCol, errorMessageCol, errorLineCol, errorColumnCol);
    }

    private void setupStyles() {
        getStyleClass().add("results-tab-pane");
        tokensTable.getStyleClass().add("data-table");
        errorsTable.getStyleClass().add("data-table");
        symbolsTable.getStyleClass().add("data-table");
        outputTextArea.getStyleClass().add("output-text-area");

        // Estilos adicionales
        languageLabel.getStyleClass().add("language-label");
        outputTextArea.setWrapText(true);
    }

    public void updateResults(AnalysisController.AnalysisResult result) {
        languageLabel.setText(result.getLanguage() != null ? result.getLanguage().getDisplayName() : "No detectado");

        ObservableList<Token> tokens = FXCollections.observableArrayList(result.getTokens());
        tokensTable.setItems(tokens);

        ObservableList<AnalysisError> errors = FXCollections.observableArrayList();
        errors.addAll(result.getLexicalErrors());
        errors.addAll(result.getSyntacticErrors());
        errors.addAll(result.getSemanticErrors());
        errorsTable.setItems(errors);

        ObservableList<Symbol> symbols = FXCollections.observableArrayList(result.getSymbolTable().values());
        symbolsTable.setItems(symbols);

        outputTextArea.clear();
        if (result.isSuccess()) {
            outputTextArea.setText(String.join("\n", result.getExecutionOutput()));
        } else {
            outputTextArea.setText("Análisis fallido. Verifique los errores.");
        }
    }

    public void clearResults() {

        languageLabel.setText("No detectado");
        tokensTable.setItems(FXCollections.observableArrayList());
        errorsTable.setItems(FXCollections.observableArrayList());
        symbolsTable.setItems(FXCollections.observableArrayList());
        outputTextArea.clear();
    }
}