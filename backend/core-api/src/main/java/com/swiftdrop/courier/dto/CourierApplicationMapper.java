package com.swiftdrop.courier.dto;

import com.swiftdrop.courier.domain.CourierApplication;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourierApplicationMapper {
    CourierApplicationResponseDto toDto(CourierApplication application);
}
