//// ---------- SyntacticAnalyzerService.java ----------
//package com.analyzer.service;
//
//import com.analyzer.model.LanguageType;
//import com.analyzer.model.Token;
//import com.analyzer.model.AnalysisError;
//import com.analyzer.service.interfaces.ISyntacticAnalyzer;
//import com.analyzer.service.interfaces.ILanguageDetector;
//import com.analyzer.service.syntactic.*;
//import java.util.*;
//
///**
// * Servicio orquestador para análisis sintáctico.
// * Delega a implementaciones específicas según el lenguaje.
// */
//public class SyntacticAnalyzerService {
//
////    private final Map<LanguageType, ISyntacticAnalyzer> analizadores;
////    private final ILanguageDetector detector;
//
////    public SyntacticAnalyzerService(ILanguageDetector detector) {
////        this.detector = detector;
////        this.analizadores = Map.of(
////                LanguageType.HTML,   new HTMLSyntacticAnalyzer(),
////                LanguageType.PYTHON, new PythonSyntacticAnalyzer(),
////                LanguageType.PLSQL,    new SQLSyntacticAnalyzer()
////        );
////    }
//
//    /**
//     * Analiza sintácticamente la lista de tokens.
//     * @param tokens tokens generados por el análisis léxico
//     * @return lista de errores sintácticos encontrados
//     */
//    public List<AnalysisError> analyzeSyntactic(List<Token> tokens) {
//        if (tokens == null || tokens.isEmpty()) {
//            return new ArrayList<>();
//        }
//
//        // Reconstruir fuente para detectar lenguaje
//        String fuente = reconstruirFuente(tokens);
//        LanguageType lenguaje = detector.detectLanguage(fuente);
//
//        // Obtener analizador específico
//        ISyntacticAnalyzer analizador = analizadores.get(lenguaje);
//        if (analizador == null) {
//            return new ArrayList<>(); // No hay analizador para este lenguaje
//        }
//
//        // Realizar análisis sintáctico
//        return analizador.analyzeSyntactic(tokens);
//    }
//
//    /**
//     * Reconstruye la fuente a partir de los tokens para detectar el lenguaje.
//     * @param tokens lista de tokens
//     * @return fuente reconstruida
//     */
//    private String reconstruirFuente(List<Token> tokens) {
//        if (tokens.isEmpty()) {
//            return "";
//        }
//
//        StringBuilder fuente = new StringBuilder();
//        int lineaActual = 1;
//
//        for (Token token : tokens) {
//            // Agregar saltos de línea si es necesario
//            while (lineaActual < token.getLine()) {
//                fuente.append("\n");
//                lineaActual++;
//            }
//
//            fuente.append(token.getValue()).append(" ");
//        }
//
//        return fuente.toString();
//    }
//
//    /**
//     * Obtiene los analizadores sintácticos disponibles.
//     * @return conjunto de tipos de lenguaje soportados
//     */
//    public Set<LanguageType> obtenerLenguajesSoportados() {
//        return analizadores.keySet();




//    }

package com.analyzer.service;

import com.analyzer.service.interfaces.ISyntacticAnalyzer;
import com.analyzer.model.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SyntacticAnalyzerService implements ISyntacticAnalyzer {

    @Override
    public List<AnalysisError> analyze(List<Token> tokens, LanguageType language) {
        List<AnalysisError> errors = new ArrayList<>();

        switch (language) {
            case HTML:
                errors.addAll(analyzeHtmlSyntax(tokens));
                break;
            case PYTHON:
                errors.addAll(analyzePythonSyntax(tokens));
                break;
            case PLSQL:
                errors.addAll(analyzePlsqlSyntax(tokens));
                break;
            default:
                // No hay análisis sintáctico para lenguajes desconocidos
                break;
        }

        return errors;
    }

    // ==============================================
    // ANÁLISIS SINTÁCTICO PARA PYTHON - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzePythonSyntax(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        // Verificar indentación
        errors.addAll(checkPythonIndentation(tokens));

        // Verificar estructuras de control
        errors.addAll(checkPythonControlStructures(tokens));

        // Verificar paréntesis y corchetes balanceados
        errors.addAll(checkBalancedBrackets(tokens));

        // Verificar definiciones de funciones y clases
        errors.addAll(checkPythonDefinitions(tokens));

        // Verificar expresiones matemáticas
        errors.addAll(checkPythonMathExpressions(tokens));

        // Verificar nombres de variables
        errors.addAll(checkPythonVariableNames(tokens));

        return errors;
    }

    private List<AnalysisError> checkPythonIndentation(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Map<Integer, Integer> lineIndentations = new HashMap<>();
        Stack<Integer> indentationStack = new Stack<>();
        indentationStack.push(0); // Nivel base

        // Calcular indentación por línea
        for (Token token : tokens) {
            if (!lineIndentations.containsKey(token.getLine())) {
                // Calcular espacios al inicio de la línea (simulado)
                int indentation = token.getColumn();
                lineIndentations.put(token.getLine(), indentation);
            }
        }

        // Verificar consistencia de indentación
        for (Token token : tokens) {
            if (token.isOfType("KEYWORD")) {
                String keyword = token.getValue();

                if (keyword.equals("def") || keyword.equals("class") ||
                        keyword.equals("if") || keyword.equals("for") ||
                        keyword.equals("while") || keyword.equals("try") ||
                        keyword.equals("except") || keyword.equals("with")) {

                    // Verificar que termine con ':'
                    if (!hasColonInSameLine(tokens, token)) {
                        errors.add(new AnalysisError(
                                "Se esperaba ':' después de '" + keyword + "'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }

                    // El siguiente bloque debe estar indentado
                    int currentIndent = lineIndentations.getOrDefault(token.getLine(), 0);
                    indentationStack.push(currentIndent + 4); // Asumir 4 espacios
                }
                else if (keyword.equals("else") || keyword.equals("elif") ||
                        keyword.equals("except") || keyword.equals("finally")) {

                    int currentIndent = lineIndentations.getOrDefault(token.getLine(), 0);
                    if (!indentationStack.isEmpty()) {
                        int expectedIndent = indentationStack.peek() - 4;
                        if (currentIndent != expectedIndent) {
                            errors.add(new AnalysisError(
                                    "Indentación incorrecta para '" + keyword + "'",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                    }
                }
            }
        }

        return errors;
    }

    private boolean hasColonInSameLine(List<Token> tokens, Token referenceToken) {
        for (Token token : tokens) {
            if (token.getLine() == referenceToken.getLine() &&
                    token.getValue().contains(":")) {
                return true;
            }
        }
        return false;
    }

    private List<AnalysisError> checkPythonControlStructures(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD")) {
                switch (token.getValue()) {
                    case "if":
                        // Verificar que no use '=>' en lugar de '=='
                        if (hasTokenInSameLine(tokens, token, "=>")) {
                            errors.add(new AnalysisError(
                                    "Operador incorrecto. Use '==' para comparación, no '=>'",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }

                        // Verificar estructura básica: if condition:
                        if (!hasBasicIfStructure(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "Estructura 'if' incompleta. Formato: if condición:",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;

                    case "for":
                        // Verificar estructura: for var in iterable:
                        if (!hasValidForStructure(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "Estructura 'for' incorrecta. Formato: for variable in iterable:",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;

                    case "while":
                        // Verificar que tenga condición
                        if (!hasConditionAfterWhile(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "Estructura 'while' sin condición",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;
                }
            }
        }

        return errors;
    }

    private boolean hasTokenInSameLine(List<Token> tokens, Token referenceToken, String searchValue) {
        return tokens.stream()
                .anyMatch(t -> t.getLine() == referenceToken.getLine() &&
                        t.getValue().equals(searchValue));
    }

    private boolean hasBasicIfStructure(List<Token> tokens, int ifIndex) {
        // Buscar ':' en la misma línea después del if
        Token ifToken = tokens.get(ifIndex);
        return tokens.stream()
                .anyMatch(t -> t.getLine() == ifToken.getLine() &&
                        t.getValue().contains(":") &&
                        t.getColumn() > ifToken.getColumn());
    }

    private boolean hasValidForStructure(List<Token> tokens, int forIndex) {
        // Verificar patrón: for [variable] in [iterable]:
        if (forIndex + 3 >= tokens.size()) return false;

        Token forToken = tokens.get(forIndex);
        boolean foundIn = false;
        boolean foundColon = false;

        for (int i = forIndex + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getLine() != forToken.getLine()) break;

            if (t.getValue().equals("in")) foundIn = true;
            if (t.getValue().contains(":")) foundColon = true;
        }

        return foundIn && foundColon;
    }

    private boolean hasConditionAfterWhile(List<Token> tokens, int whileIndex) {
        if (whileIndex + 1 >= tokens.size()) return false;

        Token whileToken = tokens.get(whileIndex);
        Token nextToken = tokens.get(whileIndex + 1);

        return nextToken.getLine() == whileToken.getLine() &&
                !nextToken.getValue().equals(":");
    }

    private List<AnalysisError> checkBalancedBrackets(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Stack<Token> stack = new Stack<>();
        Map<String, String> brackets = Map.of("(", ")", "[", "]", "{", "}");

        for (Token token : tokens) {
            String value = token.getValue();

            if (brackets.containsKey(value)) {
                // Apertura
                stack.push(token);
            } else if (brackets.containsValue(value)) {
                // Cierre
                if (stack.isEmpty()) {
                    errors.add(new AnalysisError(
                            "Paréntesis/corchete de cierre sin apertura: '" + value + "'",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                } else {
                    Token openToken = stack.pop();
                    String expectedClose = brackets.get(openToken.getValue());
                    if (!expectedClose.equals(value)) {
                        errors.add(new AnalysisError(
                                "Paréntesis/corchetes no coinciden: '" + openToken.getValue() +
                                        "' abierto en línea " + openToken.getLine() +
                                        " pero se cierra con '" + value + "'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        // Verificar paréntesis sin cerrar
        while (!stack.isEmpty()) {
            Token unclosedToken = stack.pop();
            errors.add(new AnalysisError(
                    "Paréntesis/corchete sin cerrar: '" + unclosedToken.getValue() + "'",
                    AnalysisError.ErrorType.SYNTACTIC,
                    unclosedToken.getLine(), unclosedToken.getColumn()
            ));
        }

        return errors;
    }

    private List<AnalysisError> checkPythonDefinitions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD") && token.getValue().equals("def")) {
                // Verificar estructura de función
                if (i + 1 < tokens.size()) {
                    Token nameToken = tokens.get(i + 1);
                    if (!nameToken.isOfType("IDENTIFIER")) {
                        errors.add(new AnalysisError(
                                "Se esperaba nombre de función después de 'def'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    } else {
                        // Verificar que haya paréntesis
                        boolean hasParens = false;
                        for (int j = i + 2; j < tokens.size() &&
                                tokens.get(j).getLine() == token.getLine(); j++) {
                            if (tokens.get(j).getValue().equals("(")) {
                                hasParens = true;
                                break;
                            }
                        }
                        if (!hasParens) {
                            errors.add(new AnalysisError(
                                    "Definición de función sin paréntesis",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                    }
                }
            }

            if (token.isOfType("KEYWORD") && token.getValue().equals("class")) {
                // Verificar estructura de clase
                if (i + 1 < tokens.size()) {
                    Token nameToken = tokens.get(i + 1);
                    if (!nameToken.isOfType("IDENTIFIER")) {
                        errors.add(new AnalysisError(
                                "Se esperaba nombre de clase después de 'class'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkPythonMathExpressions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size() - 1; i++) {
            Token current = tokens.get(i);
            Token next = tokens.get(i + 1);

            // Verificar operadores consecutivos
            if (current.isOfType("OPERATOR") && next.isOfType("OPERATOR") &&
                    current.getLine() == next.getLine()) {
                errors.add(new AnalysisError(
                        "Operadores consecutivos: '" + current.getValue() + " " + next.getValue() + "'",
                        AnalysisError.ErrorType.SYNTACTIC,
                        current.getLine(), current.getColumn()
                ));
            }
        }

        return errors;
    }

    private List<AnalysisError> checkPythonVariableNames(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("IDENTIFIER")) {
                String name = token.getValue();

                // Verificar espacios en nombres (esto debería detectarse en análisis léxico,
                // pero podemos verificar tokens adyacentes)
                if (i + 1 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    if (next.isOfType("IDENTIFIER") &&
                            next.getLine() == token.getLine() &&
                            next.getColumn() == token.getColumn() + token.getValue().length() + 1) {
                        errors.add(new AnalysisError(
                                "Posible espacio en nombre de variable: '" + name + " " + next.getValue() + "'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    // ==============================================
    // ANÁLISIS SINTÁCTICO PARA SQL - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzePlsqlSyntax(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        errors.addAll(checkSqlBasicStructure(tokens));
        errors.addAll(checkSqlExpressions(tokens));
        errors.addAll(checkSqlConditionals(tokens));

        return errors;
    }

    private List<AnalysisError> checkSqlBasicStructure(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.isOfType("KEYWORD")) {
                String keyword = token.getValue().toUpperCase();

                switch (keyword) {
                    case "SELECT":
                        // Verificar que haya FROM
                        if (!hasFromAfterSelect(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "SELECT sin FROM correspondiente",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }

                        // Verificar que haya columnas después de SELECT
                        if (!hasColumnsAfterSelect(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "SELECT sin especificar columnas",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;

                    case "INSERT":
                        // Verificar estructura INSERT INTO ... VALUES
                        if (!hasValidInsertStructure(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "Estructura INSERT incorrecta. Formato: INSERT INTO tabla (columnas) VALUES (valores)",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;

                    case "UPDATE":
                        // Verificar que haya SET
                        if (!hasSetAfterUpdate(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "UPDATE sin cláusula SET",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;

                    case "DELETE":
                        // Verificar que haya FROM
                        if (!hasFromAfterDelete(tokens, i)) {
                            errors.add(new AnalysisError(
                                    "DELETE sin especificar FROM tabla",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                        break;
                }
            }
        }

        return errors;
    }

    private boolean hasFromAfterSelect(List<Token> tokens, int selectIndex) {
        for (int i = selectIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).getValue().toUpperCase().equals("FROM")) {
                return true;
            }
            // Si encontramos otro SELECT, parar la búsqueda
            if (tokens.get(i).getValue().toUpperCase().equals("SELECT")) {
                break;
            }
        }
        return false;
    }

    private boolean hasColumnsAfterSelect(List<Token> tokens, int selectIndex) {
        if (selectIndex + 1 >= tokens.size()) return false;

        // Verificar que el siguiente token no sea FROM
        Token nextToken = tokens.get(selectIndex + 1);
        return !nextToken.getValue().toUpperCase().equals("FROM");
    }

    private boolean hasValidInsertStructure(List<Token> tokens, int insertIndex) {
        boolean hasInto = false;
        boolean hasValues = false;

        for (int i = insertIndex + 1; i < tokens.size(); i++) {
            String tokenValue = tokens.get(i).getValue().toUpperCase();
            if (tokenValue.equals("INTO")) hasInto = true;
            if (tokenValue.equals("VALUES")) hasValues = true;
        }

        return hasInto && hasValues;
    }

    private boolean hasSetAfterUpdate(List<Token> tokens, int updateIndex) {
        for (int i = updateIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).getValue().toUpperCase().equals("SET")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFromAfterDelete(List<Token> tokens, int deleteIndex) {
        for (int i = deleteIndex + 1; i < tokens.size(); i++) {
            if (tokens.get(i).getValue().toUpperCase().equals("FROM")) {
                return true;
            }
        }
        return false;
    }

    private List<AnalysisError> checkSqlExpressions(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            // Verificar expresiones WHERE mal formadas
            if (token.getValue().toUpperCase().equals("WHERE")) {
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);

                    // WHERE id = AND ... (falta valor)
                    if (nextToken.getValue().equals("=") || nextToken.getValue().toUpperCase().equals("AND")) {
                        errors.add(new AnalysisError(
                                "Expresión WHERE mal formada: falta condición",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }

            // Verificar operadores matemáticos consecutivos
            if (token.getValue().matches("[+\\-*/]")) {
                if (i + 1 < tokens.size()) {
                    Token nextToken = tokens.get(i + 1);
                    if (nextToken.getValue().matches("[+\\-*/]")) {
                        errors.add(new AnalysisError(
                                "Operadores matemáticos consecutivos: '" + token.getValue() + " " + nextToken.getValue() + "'",
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkSqlConditionals(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);

            if (token.getValue().toUpperCase().equals("IF")) {
                // Verificar estructura IF ... THEN ... ELSE ... END
                boolean hasThen = false;
                boolean hasEnd = false;

                for (int j = i + 1; j < tokens.size(); j++) {
                    String tokenValue = tokens.get(j).getValue().toUpperCase();
                    if (tokenValue.equals("THEN")) hasThen = true;
                    if (tokenValue.equals("END")) {
                        hasEnd = true;
                        break;
                    }
                }

                if (!hasThen) {
                    errors.add(new AnalysisError(
                            "IF sin THEN correspondiente",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                }

                if (!hasEnd) {
                    errors.add(new AnalysisError(
                            "IF sin END correspondiente",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    // ==============================================
    // ANÁLISIS SINTÁCTICO PARA HTML - MEJORADO
    // ==============================================
    private List<AnalysisError> analyzeHtmlSyntax(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        errors.addAll(checkHtmlTagNesting(tokens));
        errors.addAll(checkHtmlTagClosure(tokens));
        errors.addAll(checkHtmlAttributes(tokens));
        errors.addAll(checkHtmlDoctype(tokens));

        return errors;
    }

    private List<AnalysisError> checkHtmlTagNesting(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();
        Stack<Token> tagStack = new Stack<>();

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();

                if (tagValue.startsWith("</")) {
                    // Etiqueta de cierre
                    String tagName = extractTagName(tagValue.substring(2));

                    if (tagStack.isEmpty()) {
                        errors.add(new AnalysisError(
                                "Etiqueta de cierre sin etiqueta de apertura: " + tagValue,
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    } else {
                        Token lastOpenTag = tagStack.pop();
                        String lastOpenTagName = extractTagName(lastOpenTag.getValue().substring(1));

                        if (!lastOpenTagName.equals(tagName)) {
                            errors.add(new AnalysisError(
                                    "Etiquetas mal anidadas: esperaba </" + lastOpenTagName +
                                            "> pero encontró " + tagValue,
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                    }
                } else if (!tagValue.endsWith("/>") && !isSelfClosingTag(tagValue)) {
                    // Etiqueta de apertura
                    tagStack.push(token);
                }
            }
        }

        // Verificar etiquetas no cerradas
        while (!tagStack.isEmpty()) {
            Token unclosedTag = tagStack.pop();
            String tagName = extractTagName(unclosedTag.getValue().substring(1));
            errors.add(new AnalysisError(
                    "Etiqueta no cerrada: <" + tagName + ">",
                    AnalysisError.ErrorType.SYNTACTIC,
                    unclosedTag.getLine(), unclosedTag.getColumn()
            ));
        }

        return errors;
    }

    private List<AnalysisError> checkHtmlTagClosure(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();

                // Verificar etiquetas que requieren cierre obligatorio
                Set<String> requiresClosure = Set.of("p", "div", "span", "a", "h1", "h2", "h3", "h4", "h5", "h6");

                if (!tagValue.startsWith("</") && !tagValue.endsWith("/>")) {
                    String tagName = extractTagName(tagValue.substring(1));
                    if (requiresClosure.contains(tagName.toLowerCase())) {
                        // Verificar que tenga cierre correspondiente
                        boolean hasClosure = tokens.stream()
                                .anyMatch(t -> t.isOfType("TAG") &&
                                        t.getValue().equals("</" + tagName + ">"));

                        if (!hasClosure) {
                            errors.add(new AnalysisError(
                                    "Etiqueta obligatoria sin cierre: <" + tagName + ">",
                                    AnalysisError.ErrorType.SYNTACTIC,
                                    token.getLine(), token.getColumn()
                            ));
                        }
                    }
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkHtmlAttributes(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        for (Token token : tokens) {
            if (token.isOfType("TAG")) {
                String tagValue = token.getValue();

                // Verificar atributos sin valores o sin comillas
                Pattern attrPattern = Pattern.compile("\\s+(\\w+)\\s*=\\s*([^\"'\\s>]+)");
                Matcher matcher = attrPattern.matcher(tagValue);

                while (matcher.find()) {
                    String attrValue = matcher.group(2);
                    if (!attrValue.startsWith("\"") && !attrValue.startsWith("'")) {
                        errors.add(new AnalysisError(
                                "Valor de atributo sin comillas: " + matcher.group(1) + "=" + attrValue,
                                AnalysisError.ErrorType.SYNTACTIC,
                                token.getLine(), token.getColumn()
                        ));
                    }
                }

                // Verificar atributos sin valores
                Pattern attrNoValuePattern = Pattern.compile("\\s+(\\w+)\\s*=\\s*[>\\s]");
                matcher = attrNoValuePattern.matcher(tagValue);

                while (matcher.find()) {
                    errors.add(new AnalysisError(
                            "Atributo sin valor: " + matcher.group(1),
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
            }
        }

        return errors;
    }

    private List<AnalysisError> checkHtmlDoctype(List<Token> tokens) {
        List<AnalysisError> errors = new ArrayList<>();

        // Verificar si hay DOCTYPE y si está bien formado
        boolean hasDoctype = false;
        for (Token token : tokens) {
            if (token.getValue().toUpperCase().contains("<!DOCTYPE")) {
                hasDoctype = true;

                // Verificar formato básico
                if (!token.getValue().toLowerCase().contains("html")) {
                    errors.add(new AnalysisError(
                            "DOCTYPE incorrecto. Use: <!DOCTYPE html>",
                            AnalysisError.ErrorType.SYNTACTIC,
                            token.getLine(), token.getColumn()
                    ));
                }
                break;
            }
        }

        return errors;
    }

    private String extractTagName(String tagContent) {
        // Extraer solo el nombre de la etiqueta, sin atributos
        int spaceIndex = tagContent.indexOf(' ');
        int closeIndex = tagContent.indexOf('>');

        int endIndex = tagContent.length();
        if (spaceIndex != -1) endIndex = Math.min(endIndex, spaceIndex);
        if (closeIndex != -1) endIndex = Math.min(endIndex, closeIndex);

        return tagContent.substring(0, endIndex).trim();
    }

    private boolean isSelfClosingTag(String tagValue) {
        String tagName = extractTagName(tagValue.substring(1)).toLowerCase();
        Set<String> selfClosingTags = Set.of("br", "hr", "img", "input", "meta", "link");
        return selfClosingTags.contains(tagName);
    }
}
