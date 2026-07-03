package com.sauda.repository;

import com.sauda.domain.entity.Offer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OfferRepository extends JpaRepository<Offer, UUID>, JpaSpecificationExecutor<Offer> {

    @EntityGraph(attributePaths = "distributor")
    Optional<Offer> findWithDistributorById(UUID id);

    @EntityGraph(attributePaths = {"distributor", "canonicalProduct"})
    Optional<Offer> findWithDetailsById(UUID id);
}
