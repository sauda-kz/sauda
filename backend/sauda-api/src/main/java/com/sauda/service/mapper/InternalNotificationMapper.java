package com.sauda.service.mapper;

import com.sauda.domain.entity.InternalNotification;
import com.sauda.dto.notification.InternalNotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InternalNotificationMapper {

    @Mapping(source = "lotMatch.id", target = "lotMatchId")
    @Mapping(source = "lotMatch.lot.id", target = "lotId")
    @Mapping(source = "lotMatch.lot.title", target = "lotTitle")
    InternalNotificationResponse toResponse(InternalNotification notification);
}
