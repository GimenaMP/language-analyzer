
// --- MainApplication.java ---
package com.analyzer;

import com.analyzer.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicación
 */
public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            primaryStage.setTitle("Analizador de Lenguajes - HTML, Python, PL/SQL");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            MainView mainView = new MainView(primaryStage);
            Scene scene = mainView.createScene();

            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al iniciar la aplicación: " + e.getMessage());
        }
    }

   public static void main(String[] args) {
       launch(args);
  }
}
