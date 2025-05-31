package com.analyzer.service.LexicalAnalizer;

import java.util.*;
import com.analyzer.model.Token;
import com.analyzer.model.Symbol;
import com.analyzer.model.Symbol.SymbolType;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.ExpresionesRegulares.SQLRegexTokenizer;

/**
 * Implementación de ILexicalAnalyzer para SQL/PLSQL.
 * Utiliza SQLRegexTokenizer para tokenizar con posición, llena tabla de símbolos y reporta errores.
 */
public class SQLLexicalAnalyzer implements ILexicalAnalyzer {

    private final SQLRegexTokenizer tokenizer = new SQLRegexTokenizer();
    private List<Symbol> symbolTable;
    private List<AnalysisError> errorList;

    /**
     * Tokeniza el código SQL/PLSQL en una lista de tokens con información de línea y columna.
     */
    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

    /**
     * Realiza el análisis léxico: tokeniza, construye la tabla de símbolos y llena la lista de errores.
     * @param fuente  el código SQL/PLSQL a analizar
     * @param errores lista donde se añaden los errores detectados
     * @return lista de tokens generados
     */
    @Override
    public List<Token> analyzeLexical(String fuente, List<AnalysisError> errores) {
        List<Token> tokens = tokenizer.tokenize(fuente);
        symbolTable = new ArrayList<>();
        errorList = errores;

        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int line = token.getLine();
            int column = token.getColumn();

            // Registro de símbolos
            SymbolType symType;
            if ("IDENTIFICADOR".equals(type)) {
                symType = SymbolType.VARIABLE;
            } else if ("TABLE_NAME".equals(type)) {
                symType = SymbolType.TABLE;
            } else if ("COLUMN_NAME".equals(type)) {
                symType = SymbolType.COLUMN;
            } else if ("NUMERO".equals(type) || "CADENA".equals(type)) {
                symType = SymbolType.CONSTANT;
            } else {
                symType = SymbolType.UNKNOWN;
            }

            Symbol sym = new Symbol(lexeme, symType);
            sym.setDeclarationLine(line);
            sym.setDeclarationColumn(column);
            sym.setScope("global");
            sym.setDataType(type);
            symbolTable.add(sym);

            // Manejo de errores
            if (type.startsWith("ERROR_") || "INVALIDO".equals(type)) {
                switch (type) {
                    case "ERROR_COMENTARIO":
                        errores.add(new AnalysisError(
                            "Comentario mal formado",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_CADENA":
                        errores.add(new AnalysisError(
                            "Cadena sin cerrar",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_NUMERO":
                        errores.add(new AnalysisError(
                            "Número mal formado",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_IDENTIFICADOR":
                        errores.add(new AnalysisError(
                            "Identificador inválido",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "ERROR_OPERADOR":
                        errores.add(new AnalysisError(
                            "Operador inválido",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                    case "INVALIDO":
                        errores.add(new AnalysisError(
                            "Token desconocido: '" + lexeme + "'",
                            AnalysisError.ErrorType.LEXICAL,
                            line,
                            column
                        ));
                        break;
                }
            }
        }
        return tokens;
    }

    /**
     * Método genérico de análisis definido en ILexicalAnalyzer.
     * @param code     el código a analizar
     * @param language el lenguaje (ignorado en esta implementación)
     * @return lista de tokens generados
     */
    @Override
    public List<Token> analyze(String code, LanguageType language) {
        return analyzeLexical(code, new ArrayList<>());
    }

    /**
     * Devuelve la tabla de símbolos generada.
     * @return lista inmutable de símbolos
     */
    public List<Symbol> getSymbolTable() {
        return Collections.unmodifiableList(symbolTable);
    }

    /// Devuelve la lista de errores léxicos detectados.
    public List<AnalysisError> getErrorList() {
        return Collections.unmodifiableList(errorList);
    }

}
