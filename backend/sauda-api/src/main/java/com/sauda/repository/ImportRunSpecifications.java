package com.sauda.repository;

import com.sauda.domain.entity.ImportRun;
import com.sauda.domain.enums.ImportStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ImportRunSpecifications {

    private ImportRunSpecifications() {}

    public static Specification<ImportRun> withAdminFilters(
            UUID distributorId, ImportStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("distributor", JoinType.INNER);
                root.fetch("rawUpload", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (distributorId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("distributor").get("id"), distributorId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
