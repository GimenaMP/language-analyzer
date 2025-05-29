package com.analyzer;

import com.analyzer.controller.AnalysisController;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;

import java.util.List;

public class AnalyzerIntegrationTest {

    private AnalysisController controller;

    public void ejecutarPruebasIntegracion() {
        System.out.println("Iniciando pruebas de integración...\n");

        inicializar();
        //probarAnalisisPython();
        probarAnalisisHtml();
//        probarAnalisisPlsql();

        System.out.println("\nPruebas de integración completadas.");
    }

    private void inicializar() {
        controller = new AnalysisController();
    }

//    private void probarAnalisisPython() {
//        String codigoPython =
//                "       def suma(a, b):\n" +
//                        "result = a ++ b\n" +    // Operación correcta para suma
//                        " return result\n" +
//                        "\n" +
//                        "# Variables sin declarar\n" +
//                        "345x = c + d\n" +           // Error: c y d no están declarados
//                        "\n" +
//                        "# Reasignación de función incorporada\n" +
//                        "print = 'hola'\n" +      // Error: reasignando función incorporada
//                        "\n" +
//                        "# División por cero potencial\n" +
//                        "def divide(a, b):\n" +
//                        "    return a / 0\n" +    // Error: división por cero
//                        "\n" +
//                        "# Bucle con rango incorrecto\n" +
//                        "for i in range(10, 1):\n" + // Error: rango inválido
//                        "    print(i)\n";
//
//        AnalysisController.AnalysisResult resultado = controller.performCompleteAnalysis(codigoPython);
//
//        verificarResultado(resultado, LanguageType.PYTHON);
//    }

    private void probarAnalisisHtml() {
        System.out.println("\n=== Prueba de análisis HTML ===");
        String codigoHtml =
                "<html>\n" +
                        "  <body>\n" +
                        "    <h1>Título</h1\n" +  // Error sintáctico
                        "    <div>Contenido</span>\n" +  // Error de anidación
                        "  </body>\n" +
                        "</html>";

        AnalysisController.AnalysisResult resultado = controller.performCompleteAnalysis(codigoHtml);

        verificarResultado(resultado, LanguageType.HTML);
    }
//
//    private void probarAnalisisPlsql() {
//        System.out.println("\n=== Prueba de análisis PL/SQL ===");
//        String codigoSql =
//                "CREATE TABLE usuarios (id NUMBER, nombre VARCHAR2(50));\n" +
//                        "SELECT * FROM usuarios\n" +  // Error sintáctico: falta punto y coma
//                        "DELETE FROM usuarios;";  // Error semántico: sin WHERE
//
//        AnalysisController.AnalysisResult resultado = controller.performCompleteAnalysis(codigoSql);
//
//        verificarResultado(resultado, LanguageType.PLSQL);
//    }

    private void verificarResultado(AnalysisController.AnalysisResult resultado, LanguageType lenguajeEsperado) {
        // Verificar detección de lenguaje
        if (resultado.getLanguage() != lenguajeEsperado) {
            System.out.println("ERROR: Lenguaje detectado incorrectamente");
            return;
        }

        // Verificar tokens
        if (resultado.getTokens() == null || resultado.getTokens().isEmpty()) {
            System.out.println("ERROR: No se generaron tokens");
            return;
        }

        // Mostrar errores encontrados
        List<AnalysisError> errores = resultado.getAllErrors();
        System.out.println("\nErrores encontrados: " + errores.size());

        // Agrupar y mostrar errores por tipo
        errores.stream()
                .sorted((e1, e2) -> e1.getErrorType().compareTo(e2.getErrorType()))
                .forEach(error -> System.out.println("- " + error.getMessage() +
                        " (" + error.getErrorType().getDisplayName() +
                        ", Línea " + error.getLine() + ")")
                );

        // Verificar tabla de símbolos
        if (resultado.getSymbolTable() == null) {
            System.out.println("ERROR: No se generó tabla de símbolos");
            return;
        }

        // Mostrar salida de simulación
        System.out.println("\nSimulación de ejecución:");
        for (String linea : resultado.getExecutionOutput()) {
            System.out.println(linea);
        }

        //tabla de tokens
        System.out.println("\nTokens generados:");
        resultado.getTokens().forEach(token ->
            System.out.println("- " + token.getValue() + " (" + token.getType() + ")")
        );

        System.out.println("Prueba completada con éxito");
    }

    public static void main(String[] args) {
        AnalyzerIntegrationTest test = new AnalyzerIntegrationTest();
        test.ejecutarPruebasIntegracion();
    }
}