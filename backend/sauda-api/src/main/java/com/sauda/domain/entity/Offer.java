package com.sauda.domain.entity;

import com.sauda.domain.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "offer")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Organization distributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canonical_product_id")
    private CanonicalProduct canonicalProduct;

    @Column(name = "internal_sku")
    private String internalSku;

    @Column(name = "raw_name", nullable = false)
    private String rawName;

    private String brand;

    @Column(name = "model_mpn")
    private String modelMpn;

    @Column(precision = 18, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "KZT";

    @Column(name = "price_includes_vat")
    private Boolean priceIncludesVat;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "stock_status", nullable = false, columnDefinition = "stock_status")
    private StockStatus stockStatus = StockStatus.unknown;

    @Column(name = "lead_time")
    private String leadTime;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_file_id")
    private ImportRun sourceFile;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
