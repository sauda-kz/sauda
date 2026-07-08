package com.sauda.service;

import com.sauda.domain.entity.Offer;
import com.sauda.domain.enums.OrganizationType;
import com.sauda.dto.offer.OfferResponse;
import com.sauda.dto.offer.OfferSearchCriteria;
import com.sauda.exception.SaudaNotFoundException;
import com.sauda.repository.OfferRepository;
import com.sauda.repository.OfferSpecifications;
import com.sauda.security.principal.SaudaPrincipal;
import com.sauda.security.principal.SecurityUtils;
import com.sauda.service.mapper.OfferMapper;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferMapper offerMapper;
    private final TenantAccessService tenantAccessService;

    public OfferService(
            OfferRepository offerRepository,
            OfferMapper offerMapper,
            TenantAccessService tenantAccessService) {
        this.offerRepository = offerRepository;
        this.offerMapper = offerMapper;
        this.tenantAccessService = tenantAccessService;
    }

    @Transactional(readOnly = true)
    public Page<OfferResponse> search(OfferSearchCriteria criteria, Pageable pageable) {
        OfferSearchCriteria scopedCriteria = scopeCriteria(criteria);
        log.debug(
                "Searching offers: distributorId={}, brand={}, q={}",
                scopedCriteria.distributorId(),
                scopedCriteria.brand(),
                scopedCriteria.query());
        return offerRepository
                .findAll(OfferSpecifications.withFilters(scopedCriteria), pageable)
                .map(offerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OfferResponse getById(UUID id) {
        Offer offer =
                offerRepository
                        .findWithDetailsById(id)
                        .orElseThrow(() -> new SaudaNotFoundException("Offer not found: " + id));
        assertOfferAccess(offer);
        return offerMapper.toResponse(offer);
    }

    private OfferSearchCriteria scopeCriteria(OfferSearchCriteria criteria) {
        SaudaPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.organizationType() == OrganizationType.distributor) {
            return new OfferSearchCriteria(
                    principal.organizationId(),
                    criteria.category(),
                    criteria.brand(),
                    criteria.query(),
                    criteria.stockStatus(),
                    criteria.activeOnly());
        }
        return criteria;
    }

    private void assertOfferAccess(Offer offer) {
        SaudaPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.organizationType() == OrganizationType.distributor) {
            tenantAccessService.assertCurrentOrganization(offer.getDistributor().getId());
        }
    }
}
