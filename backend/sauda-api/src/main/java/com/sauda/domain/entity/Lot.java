package com.sauda.domain.entity;

import com.sauda.domain.enums.LotStatus;
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
import java.util.HashMap;
import java.util.Map;
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
@Table(name = "lot")
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String source;

    @Column(name = "external_purchase_id")
    private String externalPurchaseId;

    @Column(name = "external_lot_id")
    private String externalLotId;

    private String title;

    @Column(name = "customer_name")
    private String customerName;

    private String category;

    private String description;

    @Column(name = "procurement_method")
    private String procurementMethod;

    @Column(name = "lot_type")
    private String lotType;

    private Integer quantity;

    private String unit;

    @Column(name = "budget_amount", precision = 18, scale = 2)
    private BigDecimal budgetAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency = "KZT";

    @Column(name = "delivery_location")
    private String deliveryLocation;

    @Column(name = "delivery_deadline")
    private Instant deliveryDeadline;

    @Column(name = "submission_deadline")
    private Instant submissionDeadline;

    @Column(name = "warranty_requirements")
    private String warrantyRequirements;

    @Column(name = "technical_requirements")
    private String technicalRequirements;

    @Column(name = "required_documents")
    private String requiredDocuments;

    @Column(name = "qualification_requirements")
    private String qualificationRequirements;

    @Column(name = "contract_terms_summary")
    private String contractTermsSummary;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "lot_status")
    private LotStatus status = LotStatus.active;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "raw_text")
    private String rawText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawData = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
