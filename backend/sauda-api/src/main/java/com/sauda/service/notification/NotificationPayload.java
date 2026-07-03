package com.sauda.service.notification;

import com.sauda.domain.entity.AppUser;
import com.sauda.domain.entity.LotMatch;
import java.util.List;

/** Channel-agnostic notification content resolved once and delivered through each channel. */
public record NotificationPayload(
        LotMatch lotMatch, List<AppUser> recipients, String title, String message) {}
