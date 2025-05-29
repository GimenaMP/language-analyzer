

module com.analyzer {
        requires javafx.controls;
        requires javafx.fxml;
        requires java.desktop;
    requires org.junit.jupiter.api;

    exports com.analyzer;
        exports com.analyzer.model;
        exports com.analyzer.service;
        exports com.analyzer.controller;
        exports com.analyzer.view;
        exports com.analyzer.util;
}