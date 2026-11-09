package com.swiftdrop.merchant.dto;

import com.swiftdrop.merchant.domain.MerchantOrder;
import com.swiftdrop.parcel.domain.Parcel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MerchantOrderMapper {
    @Mapping(target = "trackingNumber", source = "parcel.trackingNumber")
    @Mapping(target = "status", source = "parcel.status")
    @Mapping(target = "fee", source = "parcel.fee")
    @Mapping(target = "createdAt", source = "parcel.createdAt")
    @Mapping(target = "externalOrderId", source = "order.externalOrderId")
    MerchantOrderResponse toResponse(MerchantOrder order, Parcel parcel);
}
