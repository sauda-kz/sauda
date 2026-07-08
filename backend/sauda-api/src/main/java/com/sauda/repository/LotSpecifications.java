package com.sauda.repository;

import com.sauda.domain.entity.Lot;
import com.sauda.domain.enums.LotStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class LotSpecifications {

    private LotSpecifications() {}

    public static Specification<Lot> withFilters(
            LotStatus status, String category, String source, String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(source)) {
                predicates.add(criteriaBuilder.equal(root.get("source"), source));
            }
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("title")), pattern),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("customerName")), pattern),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("externalLotId")),
                                        pattern)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
