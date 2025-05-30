package com.analyzer.service.LexicalAnalizer;

import java.util.*;

import com.analyzer.model.Token;
import com.analyzer.model.AnalysisError;
import com.analyzer.model.LanguageType;
import com.analyzer.service.interfaces.ILexicalAnalyzer;
import com.analyzer.model.ExpresionesRegulares.HTMLRegexTokenizer;

/**
 * Implementación de ILexicalAnalyzer para HTML.
 * Se apoya en expresiones regulares para tokenizar y detectar errores.
 */
public class HTMLLexicalAnalyzer implements ILexicalAnalyzer {

    private final HTMLRegexTokenizer tokenizer = new HTMLRegexTokenizer();
    private List<AnalysisError> errorList;

    @Override
    public List<Token> tokenize(String code) {
        return tokenizer.tokenize(code);
    }

    @Override
    public List<Token> analyzeLexical(String source, List<AnalysisError> errors) {
        List<Token> tokens = tokenizer.tokenize(source);
        errorList = errors;

        for (Token token : tokens) {
            String type = token.getType();
            String lexeme = token.getValue();
            int line = token.getLine();
            int column = token.getColumn();

            // Manejo de errores léxicos
            if (type.startsWith("ERROR_") || "INVALIDO".equals(type)||"TAG_RESERVADA_ABIERTA".equals(type)) {
                String mensaje;
                switch (type) {
                    case "ERROR_COMENTARIO":
                        mensaje = "Comentario mal cerrado"; break;
                    case "ERROR_DOCTYPE":
                        mensaje = "DOCTYPE mal escrito"; break;
                    case "ERROR_ATRIBUTO":
                        mensaje = "Atributo mal formado"; break;
                    case "ERROR_ENTIDAD":
                        mensaje = "Entidad HTML mal formada"; break;
                    case "INVALIDO":
                        mensaje = "Carácter inválido: '" + lexeme + "'"; break;
                    case "TAG_RESERVADA_ABIERTA":
                        mensaje = "Etiqueta reservada falta cierre > '" + lexeme + "'"; break;
                    default:
                        mensaje = "Error léxico desconocido"; break;
                }
                errors.add(new AnalysisError(mensaje, AnalysisError.ErrorType.LEXICAL, line, column));
            }
        }

        return tokens;
    }

    @Override
    public List<Token> analyze(String code, LanguageType language) {
        return analyzeLexical(code, new ArrayList<>());
    }

    public List<AnalysisError> getErrorList() {
        return Collections.unmodifiableList(errorList);
    }
}
