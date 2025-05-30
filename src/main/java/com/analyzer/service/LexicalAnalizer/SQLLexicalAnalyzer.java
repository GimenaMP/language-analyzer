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

        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int line = token.getLine();
            int column = token.getColumn();

            // Registrar símbolo según tipo de token
            SymbolType symType;
            if ("IDENTIFICADOR".equals(type) ) {
                symType = SymbolType.VARIABLE;
            } else if ("NUMERO".equals(type) || "CADENA".equals(type)) {
                symType = SymbolType.CONSTANT;
            } else {
                symType = SymbolType.UNKNOWN;
            }
            Symbol sym = new Symbol(lexeme, symType);
            sym.setDeclarationLine(line);
            sym.setDeclarationColumn(column);
            symbolTable.add(sym);

            // Detectar y reportar errores léxicos
            if (type.startsWith("ERROR_") || "INVALIDO".equals(type)) {
                String description;
                switch (type) {
                    case "ERROR_COMENTARIO":    description = "Comentario mal formado";     break;
                    case "ERROR_CADENA":        description = "Cadena sin cerrar";          break;
                    case "ERROR_NUMERO":        description = "Número mal formado";          break;
                    case "ERROR_IDENTIFICADOR": description = "Identificador inválido";     break;
                    case "ERROR_OPERADOR":      description = "Operador inválido";          break;
                    case "INVALIDO":            description = "Token desconocido";           break;
                    default:                     description = "Error léxico en token";      break;
                }
                errores.add(new AnalysisError(
                        String.format("%s: '%s' (línea %d, columna %d)", description, lexeme, line, column),
                        AnalysisError.ErrorType.LEXICAL,
                        line,
                        column
                ));
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

    }

