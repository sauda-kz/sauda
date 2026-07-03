package com.sauda.repository;

import com.sauda.domain.entity.LotMatch;
import com.sauda.domain.enums.LotMatchStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotMatchRepository extends JpaRepository<LotMatch, UUID> {

    long countByLotId(UUID lotId);

    @Query(
            """
            SELECT lm.lot.id AS lotId, COUNT(lm) AS matchCount
            FROM LotMatch lm
            WHERE lm.lot.id IN :lotIds
            GROUP BY lm.lot.id
            """)
    List<LotMatchCountView> countMatchesByLotIds(@Param("lotIds") Collection<UUID> lotIds);

    boolean existsByLotIdAndOfferId(UUID lotId, UUID offerId);

    @Query("SELECT lm.offer.id FROM LotMatch lm WHERE lm.lot.id = :lotId")
    List<UUID> findMatchedOfferIdsByLotId(@Param("lotId") UUID lotId);

    @EntityGraph(attributePaths = {"lot", "offer", "distributor"})
    Optional<LotMatch> findById(UUID id);

    @EntityGraph(attributePaths = {"lot", "offer", "distributor"})
    Page<LotMatch> findByLotId(UUID lotId, Pageable pageable);

    Page<LotMatch> findByDistributorId(UUID distributorId, Pageable pageable);

    Page<LotMatch> findByDistributorIdAndMatchStatus(
            UUID distributorId, LotMatchStatus matchStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"lot", "offer", "distributor"})
    Optional<LotMatch> findByIdAndDistributorId(UUID id, UUID distributorId);

    @EntityGraph(attributePaths = {"lot", "offer", "distributor"})
    Page<LotMatch> findByDistributorIdOrderByCreatedAtDesc(UUID distributorId, Pageable pageable);

    @EntityGraph(attributePaths = {"lot", "offer", "distributor"})
    Page<LotMatch> findByDistributorIdAndMatchStatusOrderByCreatedAtDesc(
            UUID distributorId, LotMatchStatus matchStatus, Pageable pageable);
}
