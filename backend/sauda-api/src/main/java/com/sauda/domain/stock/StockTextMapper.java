package com.sauda.domain.stock;

import com.sauda.domain.enums.StockStatus;
import java.util.Locale;
import java.util.Set;

/**
 * Maps raw stock text from price lists to structured {@link StockStatus} and optional quantity.
 * Used by future import adapters (SAUDA-008/009); no Spring dependencies.
 */
public final class StockTextMapper {

    public static final String ON_ORDER_RISK_FLAG = "on_order_delivery_unconfirmed";

    private static final Set<String> IN_STOCK_PHRASES =
            Set.of("много", "в наличии", "есть", "available", "in stock");

    private static final Set<String> LOW_STOCK_PHRASES =
            Set.of("мало", "ограничено", "остаток малый", "limited", "low stock");

    private static final Set<String> OUT_OF_STOCK_PHRASES =
            Set.of("нет", "отсутствует", "out of stock", "нет в наличии");

    private static final Set<String> ON_ORDER_PHRASES =
            Set.of("под заказ", "ожидается", "по запросу", "on order", "backorder");

    private StockTextMapper() {}

    public record StockParseResult(Integer quantity, StockStatus status) {}

    public static StockParseResult parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new StockParseResult(null, StockStatus.unknown);
        }

        String normalized = normalize(rawText);

        Integer parsedQuantity = parseQuantity(normalized);
        if (parsedQuantity != null) {
            if (parsedQuantity == 0) {
                return new StockParseResult(0, StockStatus.out_of_stock);
            }
            return new StockParseResult(parsedQuantity, StockStatus.in_stock);
        }

        StockStatus status = parseStatus(normalized);
        return new StockParseResult(null, status);
    }

    public static Integer parseQuantity(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        String normalized = normalize(rawText);
        if (!normalized.matches("-?\\d+")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static StockStatus parseStatus(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return StockStatus.unknown;
        }

        String normalized = normalize(rawText);

        if (matchesAny(normalized, ON_ORDER_PHRASES)) {
            return StockStatus.on_order;
        }
        if (matchesAny(normalized, OUT_OF_STOCK_PHRASES) || "0".equals(normalized)) {
            return StockStatus.out_of_stock;
        }
        if (matchesAny(normalized, LOW_STOCK_PHRASES)) {
            return StockStatus.low_stock;
        }
        if (matchesAny(normalized, IN_STOCK_PHRASES)) {
            return StockStatus.in_stock;
        }

        return StockStatus.unknown;
    }

    private static boolean matchesAny(String normalized, Set<String> phrases) {
        for (String phrase : phrases) {
            if (normalized.equals(phrase) || normalized.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String rawText) {
        return rawText.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }
}
