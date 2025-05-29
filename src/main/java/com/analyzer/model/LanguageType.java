package com.analyzer.model;

public enum LanguageType {
    HTML("HTML", "Lenguaje de marcado para páginas web"),
    PYTHON("Python", "Lenguaje de programación interpretado"),
    PLSQL("PL/SQL", "Lenguaje de programación de Oracle Database"),
    UNKNOWN("Desconocido", "Lenguaje no identificado");

    private final String displayName;
    private final String description;

    LanguageType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static LanguageType fromString(String languageName) {
        if (languageName == null) return UNKNOWN;
        for (LanguageType type : values()) {
            if (type.displayName.equalsIgnoreCase(languageName) ||
                    type.name().equalsIgnoreCase(languageName)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() { return displayName; }
}