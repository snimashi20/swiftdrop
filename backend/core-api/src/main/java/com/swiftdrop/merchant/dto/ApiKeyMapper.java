package com.swiftdrop.merchant.dto;

import com.swiftdrop.merchant.domain.ApiKey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ApiKeyMapper {

    @Mapping(target = "merchantId", source = "merchant.id")
    ApiKeyResponse toResponse(ApiKey apiKey);

    List<ApiKeyResponse> toResponseList(List<ApiKey> apiKeys);

    @Mapping(target = "id", source = "apiKey.id")
    @Mapping(target = "merchantId", source = "apiKey.merchant.id")
    @Mapping(target = "label", source = "apiKey.label")
    @Mapping(target = "keyPrefix", source = "apiKey.keyPrefix")
    @Mapping(target = "active", source = "apiKey.active")
    @Mapping(target = "createdAt", source = "apiKey.createdAt")
    @Mapping(target = "apiKey", source = "rawKey")
    ApiKeyGeneratedResponse toGeneratedResponse(ApiKey apiKey, String rawKey);
}
