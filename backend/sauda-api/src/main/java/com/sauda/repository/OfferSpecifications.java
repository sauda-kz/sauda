package com.sauda.repository;

import com.sauda.domain.entity.CanonicalProduct;
import com.sauda.domain.entity.Offer;
import com.sauda.dto.offer.OfferSearchCriteria;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class OfferSpecifications {

    private OfferSpecifications() {}

    public static Specification<Offer> withFilters(OfferSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("distributor", JoinType.INNER);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            Join<Offer, CanonicalProduct> canonicalJoin =
                    root.join("canonicalProduct", JoinType.LEFT);

            if (criteria.distributorId() != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("distributor").get("id"), criteria.distributorId()));
            }
            if (StringUtils.hasText(criteria.category())) {
                predicates.add(
                        criteriaBuilder.equal(
                                canonicalJoin.get("category"), criteria.category().trim()));
            }
            if (StringUtils.hasText(criteria.brand())) {
                String pattern = "%" + criteria.brand().trim().toLowerCase() + "%";
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("brand")), pattern));
            }
            if (StringUtils.hasText(criteria.query())) {
                String pattern = "%" + criteria.query().trim().toLowerCase() + "%";
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("rawName")), pattern));
            }
            if (criteria.stockStatus() != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("stockStatus"), criteria.stockStatus()));
            }
            if (criteria.activeOnly()) {
                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.isNull(root.get("canonicalProduct")),
                                criteriaBuilder.isTrue(canonicalJoin.get("active"))));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<Offer> suggestionCandidates(
            String category, Collection<UUID> excludedOfferIds) {
        return (root, query, criteriaBuilder) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("distributor", JoinType.INNER);
                root.fetch("canonicalProduct", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            Join<Offer, CanonicalProduct> canonicalJoin =
                    root.join("canonicalProduct", JoinType.LEFT);

            predicates.add(
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("canonicalProduct")),
                            criteriaBuilder.isTrue(canonicalJoin.get("active"))));

            if (StringUtils.hasText(category)) {
                predicates.add(
                        criteriaBuilder.equal(canonicalJoin.get("category"), category.trim()));
            }
            if (excludedOfferIds != null && !excludedOfferIds.isEmpty()) {
                predicates.add(criteriaBuilder.not(root.get("id").in(excludedOfferIds)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
