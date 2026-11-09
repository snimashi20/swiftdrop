package com.swiftdrop.merchant.dto;

import com.swiftdrop.parcel.dto.CreateParcelRequest;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantOrderRequest extends CreateParcelRequest {

    @Size(max = 100)
    private String externalOrderId;
}
