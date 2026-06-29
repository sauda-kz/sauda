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
import java.util.ArrayList;
import java.util.List;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_requirements", columnDefinition = "jsonb")
    private List<String> matchedRequirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_requirements", columnDefinition = "jsonb")
    private List<String> missingRequirements = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_flags", columnDefinition = "jsonb")
    private List<String> riskFlags = new ArrayList<>();

    @Column(name = "required_quantity", nullable = false)
    private int requiredQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

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

    @Column(name = "estimated_unit_price", precision = 18, scale = 2)
    private BigDecimal estimatedUnitPrice;

    @Column(name = "estimated_total_price", precision = 18, scale = 2)
    private BigDecimal estimatedTotalPrice;

    @Column(name = "budget_amount", precision = 18, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "estimated_margin", precision = 18, scale = 2)
    private BigDecimal estimatedMargin;

    @Column(name = "needs_manual_review", nullable = false)
    private boolean needsManualReview = true;

    @Column(name = "admin_comment", nullable = false)
    private String adminComment = "";

    @Column(name = "distributor_comment")
    private String distributorComment;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
