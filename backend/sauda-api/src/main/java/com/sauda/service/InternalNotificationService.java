package com.sauda.service;

import com.sauda.domain.entity.LotMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InternalNotificationService {

    /**
     * Creates in-app notifications for distributor managers. Full implementation in SAUDA-070
     * step 07.
     */
    public void notifyLotSent(LotMatch match) {
        log.info(
                "Lot sent notification scheduled: matchId={}, distributorId={}",
                match.getId(),
                match.getDistributor().getId());
    }
}
