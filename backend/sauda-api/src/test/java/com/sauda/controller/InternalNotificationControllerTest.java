package com.sauda.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sauda.domain.enums.NotificationStatus;
import com.sauda.dto.notification.InternalNotificationResponse;
import com.sauda.repository.AppUserRepository;
import com.sauda.service.InternalNotificationService;
import com.sauda.testsupport.WebMvcSecurityTestConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
class InternalNotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private InternalNotificationService internalNotificationService;
    @MockitoBean private AppUserRepository appUserRepository;

    @Test
    @WithMockUser(authorities = "notification:read")
    void listReturnsPage() throws Exception {
        UUID id = UUID.randomUUID();
        when(internalNotificationService.listForCurrentUser(eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(id, NotificationStatus.unread))));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].lotTitle").value("SSD 1TB"));
    }

    @Test
    @WithMockUser(authorities = "notification:read")
    void unreadCountReturnsNumber() throws Exception {
        when(internalNotificationService.countUnread()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    @WithMockUser(authorities = "notification:read")
    void markAsReadReturnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        when(internalNotificationService.markAsRead(id))
                .thenReturn(sampleResponse(id, NotificationStatus.read));

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("read"));
    }

    private static InternalNotificationResponse sampleResponse(UUID id, NotificationStatus status) {
        return new InternalNotificationResponse(
                id,
                "Новый подходящий лот",
                "Для вашей компании найден новый потенциально подходящий лот.",
                status,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SSD 1TB",
                Instant.now(),
                status == NotificationStatus.read ? Instant.now() : null);
    }
}
