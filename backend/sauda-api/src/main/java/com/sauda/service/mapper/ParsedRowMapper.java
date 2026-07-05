package com.sauda.service.mapper;

import com.sauda.domain.entity.ParsedRow;
import com.sauda.dto.imports.ParsedRowErrorItem;
import com.sauda.dto.imports.ParsedRowResponse;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParsedRowMapper {

    @Mapping(source = "importRun.id", target = "importRunId")
    @Mapping(source = "errors", target = "errors")
    @Mapping(source = "warnings", target = "warnings")
    ParsedRowResponse toResponse(ParsedRow parsedRow);

    default List<ParsedRowErrorItem> mapErrorItems(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(this::toErrorItem).toList();
    }

    default ParsedRowErrorItem toErrorItem(Map<String, Object> item) {
        if (item == null) {
            return new ParsedRowErrorItem(null, null, null);
        }
        return new ParsedRowErrorItem(
                stringValue(item.get("field")),
                stringValue(item.get("code")),
                stringValue(item.get("message")));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
