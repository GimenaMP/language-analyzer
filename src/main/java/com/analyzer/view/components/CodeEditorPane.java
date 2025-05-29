// --- CodeEditorPane.java ---
package com.analyzer.view.components;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

public class CodeEditorPane extends VBox {

    private TextArea codeTextArea;
    private Label statusLabel;

    public CodeEditorPane() {
        initializeComponents();
        setupLayout();
        setupStyles();
    }

    private void initializeComponents() {
        codeTextArea = new TextArea();
        codeTextArea.setPromptText("Escriba o pegue su código aquí...\n\nEjemplos:\n" +
                "HTML: <html><body><h1>Hola</h1></body></html>\n" +
                "Python: def hello():\n    print('Hola mundo')\n" +
                "PL/SQL: CREATE TABLE usuarios (id NUMBER, nombre VARCHAR2(50));");
        codeTextArea.setPrefRowCount(25);
        codeTextArea.setWrapText(true);

        statusLabel = new Label("Listo para analizar");
        statusLabel.getStyleClass().add("status-label");
    }

    private void setupLayout() {
        setPadding(new Insets(10));
        setSpacing(10);

        Label titleLabel = new Label("Editor de Código");
        titleLabel.getStyleClass().add("section-title");

        getChildren().addAll(titleLabel, codeTextArea, statusLabel);
        VBox.setVgrow(codeTextArea, Priority.ALWAYS);
    }

    private void setupStyles() {
        getStyleClass().add("code-editor-pane");
        codeTextArea.getStyleClass().add("code-text-area");
    }

    public String getCode() {
        return codeTextArea.getText();
    }

    public void setCode(String code) {
        codeTextArea.setText(code);
    }

    public void clearCode() {
        codeTextArea.clear();
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public TextArea getTextArea() {
        return codeTextArea;
    }
}
