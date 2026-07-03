package com.sauda.controller;

import com.sauda.common.ApiConstants;
import com.sauda.domain.enums.NotificationStatus;
import com.sauda.dto.notification.InternalNotificationResponse;
import com.sauda.service.InternalNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Notifications", description = "In-app notifications for the current user")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(ApiConstants.API_V1 + "/notifications")
public class InternalNotificationController {

    private final InternalNotificationService internalNotificationService;

    public InternalNotificationController(
            InternalNotificationService internalNotificationService) {
        this.internalNotificationService = internalNotificationService;
    }

    @Operation(summary = "List current user notifications")
    @GetMapping
    @PreAuthorize("hasAuthority('notification:read')")
    public Page<InternalNotificationResponse> list(
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return internalNotificationService.listForCurrentUser(status, pageable);
    }

    @Operation(summary = "Count unread notifications")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('notification:read')")
    public long unreadCount() {
        return internalNotificationService.countUnread();
    }

    @Operation(summary = "Mark notification as read")
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:read')")
    public InternalNotificationResponse markAsRead(@PathVariable UUID id) {
        return internalNotificationService.markAsRead(id);
    }
}
