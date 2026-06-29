package com.sauda.domain.stock;

import static org.assertj.core.api.Assertions.assertThat;

import com.sauda.domain.enums.StockStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockTextMapperTest {

    @ParameterizedTest
    @CsvSource({
        "в наличии, in_stock",
        "В НАЛИЧИИ, in_stock",
        "много, in_stock",
        "есть, in_stock",
        "мало, low_stock",
        "ограничено, low_stock",
        "остаток малый, low_stock",
        "нет, out_of_stock",
        "отсутствует, out_of_stock",
        "0, out_of_stock",
        "под заказ, on_order",
        "ожидается, on_order",
        "по запросу, on_order",
        "'', unknown",
        "   , unknown",
        "непонятно, unknown"
    })
    void parseStatusMapsTextToStockStatus(String input, String expected) {
        StockStatus expectedStatus = StockStatus.valueOf(expected);
        assertThat(StockTextMapper.parseStatus(input)).isEqualTo(expectedStatus);
    }

    @Test
    void parseQuantityReturnsIntegerForNumericText() {
        assertThat(StockTextMapper.parseQuantity("15")).isEqualTo(15);
        assertThat(StockTextMapper.parseQuantity("0")).isZero();
        assertThat(StockTextMapper.parseQuantity("в наличии")).isNull();
    }

    @Test
    void parseMapsPositiveNumberToInStockWithQuantity() {
        var result = StockTextMapper.parse("42");
        assertThat(result.quantity()).isEqualTo(42);
        assertThat(result.status()).isEqualTo(StockStatus.in_stock);
    }

    @Test
    void parseMapsZeroToOutOfStockWithQuantity() {
        var result = StockTextMapper.parse("0");
        assertThat(result.quantity()).isZero();
        assertThat(result.status()).isEqualTo(StockStatus.out_of_stock);
    }

    @Test
    void parseMapsPhraseWithoutQuantity() {
        var result = StockTextMapper.parse("под заказ");
        assertThat(result.quantity()).isNull();
        assertThat(result.status()).isEqualTo(StockStatus.on_order);
    }

    @Test
    void parseMapsBlankToUnknown() {
        var result = StockTextMapper.parse("   ");
        assertThat(result.quantity()).isNull();
        assertThat(result.status()).isEqualTo(StockStatus.unknown);
    }

    @Test
    void normalizeHandlesYoLetter() {
        assertThat(StockTextMapper.parseStatus("есть")).isEqualTo(StockStatus.in_stock);
    }
}
