package com.analyzer.service.interfaces;

import com.analyzer.model.LanguageType;

public interface ILanguageDetector {
    LanguageType detectLanguage(String code);
}
