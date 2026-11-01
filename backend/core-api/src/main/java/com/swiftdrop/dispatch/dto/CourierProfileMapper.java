package com.swiftdrop.dispatch.dto;

import com.swiftdrop.dispatch.domain.CourierProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourierProfileMapper {

    CourierProfileResponse toResponse(CourierProfile entity);
}