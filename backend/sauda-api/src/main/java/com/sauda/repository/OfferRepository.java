package com.sauda.repository;

import com.sauda.domain.entity.Offer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @EntityGraph(attributePaths = "distributor")
    Optional<Offer> findWithDistributorById(UUID id);
}
