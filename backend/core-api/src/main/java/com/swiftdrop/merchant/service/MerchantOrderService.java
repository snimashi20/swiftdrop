package com.swiftdrop.merchant.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.merchant.domain.MerchantOrder;
import com.swiftdrop.merchant.dto.MerchantOrderMapper;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.merchant.dto.MerchantOrderResponse;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantOrderService {
    private final MerchantOrderWriter writer;
    private final MerchantOrderRepository orderRepository;
    private final ParcelRepository parcelRepository;
    private final MerchantOrderMapper mapper;

    public CreationResult create(Long merchantId, String key, MerchantOrderRequest request) {
        MerchantOrder order;
        try {
            order = writer.create(merchantId, key, request);
        } catch (DataIntegrityViolationException ex) {
            return new CreationResult(findByKey(merchantId, key), false);
        }
        return new CreationResult(toResponse(order), true);
    }

    public BulkCreationResult createBulk(Long merchantId, String key,
                                         List<MerchantOrderRequest> requests) {
        List<MerchantOrder> orders;
        try {
            orders = writer.createBulk(merchantId, key, requests);
        } catch (DataIntegrityViolationException ex) {
            return new BulkCreationResult(findBulk(merchantId, key, requests.size()), false);
        }
        return new BulkCreationResult(orders.stream().map(this::toResponse).toList(), true);
    }

    @Transactional(readOnly = true)
    public MerchantOrderResponse get(Long merchantId, String trackingNumber) {
        MerchantOrder order = orderRepository.findByMerchantIdAndParcelTrackingNumber(merchantId, trackingNumber)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<MerchantOrderResponse> list(Long merchantId, Pageable pageable) {
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<MerchantOrderResponse> listDashboard(Long merchantId, ParcelStatus status, Pageable pageable) {
        return orderRepository.findDashboardOrders(merchantId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public java.util.Map<ParcelStatus, Long> countDashboardByStatus(Long merchantId) {
        java.util.Map<ParcelStatus, Long> counts = new java.util.EnumMap<>(ParcelStatus.class);
        for (ParcelStatus status : ParcelStatus.values()) counts.put(status, 0L);
        for (Object[] row : orderRepository.countDashboardOrdersByStatus(merchantId)) {
            counts.put((ParcelStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    private MerchantOrderResponse findByKey(Long merchantId, String key) {
        return toResponse(orderRepository.findByMerchantIdAndIdempotencyKey(merchantId, key)
                .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    private List<MerchantOrderResponse> findBulk(Long merchantId, String key, int size) {
        return IntStream.range(0, size)
                .mapToObj(index -> findByKey(merchantId, bulkKey(key, index)))
                .toList();
    }

    private MerchantOrderResponse toResponse(MerchantOrder order) {
        Parcel parcel = parcelRepository.findById(order.getParcelId())
                .orElseThrow(() -> new NotFoundException("Order parcel not found"));
        return mapper.toResponse(order, parcel);
    }

    static String bulkKey(String key, int index) {
        return key + ":" + index;
    }

    public record CreationResult(MerchantOrderResponse response, boolean created) {}
    public record BulkCreationResult(List<MerchantOrderResponse> responses, boolean created) {}
}
