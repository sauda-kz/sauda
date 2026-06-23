package com.sauda.domain.entity;

import com.sauda.domain.enums.CheckResult;
import com.sauda.domain.enums.LotMatchStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "lot_match",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_lot_offer",
                        columnNames = {"lot_id", "offer_id"}))
public class LotMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Organization distributor;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "match_status", nullable = false, columnDefinition = "lot_match_status")
    private LotMatchStatus matchStatus = LotMatchStatus.suggested;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "match_reason")
    private String matchReason;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "quantity_check", nullable = false, columnDefinition = "check_result")
    private CheckResult quantityCheck = CheckResult.unknown;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "stock_check", nullable = false, columnDefinition = "check_result")
    private CheckResult stockCheck = CheckResult.unknown;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "price_check", nullable = false, columnDefinition = "check_result")
    private CheckResult priceCheck = CheckResult.unknown;

    @Column(name = "estimated_margin", precision = 14, scale = 2)
    private BigDecimal estimatedMargin;

    @Column(name = "needs_manual_review", nullable = false)
    private boolean needsManualReview = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
