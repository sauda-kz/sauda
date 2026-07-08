package com.sauda.service.imports;

import com.sauda.domain.enums.ParsedRowStatus;
import com.sauda.service.imports.model.AdapterParsedRow;
import org.springframework.stereotype.Component;

@Component
public class ParsedRowValidator {

    public ParsedRowStatus resolveStatus(AdapterParsedRow row) {
        if (!row.errors().isEmpty()) {
            return ParsedRowStatus.error;
        }
        if (!row.warnings().isEmpty()) {
            return ParsedRowStatus.needs_review;
        }
        return ParsedRowStatus.valid;
    }

    public ParsedRowStatus resolveEditedStatus(AdapterParsedRow row) {
        if (!row.errors().isEmpty()) {
            return ParsedRowStatus.error;
        }
        if (!row.warnings().isEmpty()) {
            return ParsedRowStatus.needs_review;
        }
        return ParsedRowStatus.edited;
    }
}
