package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantOrder;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.common.exception.ValidationException;
import com.swiftdrop.parcel.service.ParcelService;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantOrderWriter {
    private final ParcelService parcelService;
    private final MerchantRepository merchantRepository;
    private final MerchantOrderRepository merchantOrderRepository;

    @Transactional
    public MerchantOrder create(Long merchantId, String idempotencyKey, MerchantOrderRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new com.swiftdrop.common.exception.NotFoundException("Merchant not found"));
        var parcel = createParcel(merchantId, request);
        return merchantOrderRepository.save(MerchantOrder.builder()
                .merchant(merchant)
                .parcelId(parcel.getId())
                .idempotencyKey(idempotencyKey)
                .externalOrderId(request.getExternalOrderId())
                .build());
    }

    private com.swiftdrop.parcel.dto.ParcelResponse createParcel(
            Long merchantId, MerchantOrderRequest request) {
        try {
            return parcelService.createForMerchant(merchantId, request);
        } catch (NotFoundException ex) {
            throw new ValidationException("Unknown pickup or dropoff district");
        }
    }

    @Transactional
    public List<MerchantOrder> createBulk(Long merchantId, String idempotencyKey,
                                           List<MerchantOrderRequest> requests) {
        return IntStream.range(0, requests.size())
                .mapToObj(index -> create(merchantId,
                        MerchantOrderService.bulkKey(idempotencyKey, index), requests.get(index)))
                .toList();
    }
}
