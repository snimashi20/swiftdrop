package com.swiftdrop.merchant.dto;

import com.swiftdrop.merchant.domain.Merchant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MerchantMapper {
    MerchantResponseDto toDto(Merchant merchant);
}
