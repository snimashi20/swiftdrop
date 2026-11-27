package com.swiftdrop.merchant.service;

import com.swiftdrop.common.exception.ConflictException;
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
import java.util.Objects;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        return toResponsePage(orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, pageable));
    }

    @Transactional(readOnly = true)
    public Page<MerchantOrderResponse> listDashboard(Long merchantId, ParcelStatus status, String search,
                                                      Pageable pageable) {
        var orders = search == null || search.isBlank()
                ? orderRepository.findDashboardOrders(merchantId, status, pageable)
                : orderRepository.searchDashboardOrders(merchantId, status, escapeLikePattern(search), pageable);
        return toResponsePage(orders);
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

    @Transactional(readOnly = true)
    public List<DailyOrderCount> getCreationTrend(Long merchantId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(6);
        Instant since = firstDay.atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<LocalDate, Long> countsByDate = new HashMap<>();
        for (Object[] row : orderRepository.countOrdersByUtcDateSince(merchantId, since)) {
            LocalDate date = row[0] instanceof java.sql.Date dateValue
                    ? dateValue.toLocalDate()
                    : (LocalDate) row[0];
            countsByDate.put(date, ((Number) row[1]).longValue());
        }
        return firstDay.datesUntil(today.plusDays(1))
                .map(date -> new DailyOrderCount(date, countsByDate.getOrDefault(date, 0L)))
                .toList();
    }

    private MerchantOrderResponse findByKey(Long merchantId, String key) {
        return toResponse(orderRepository.findByMerchantIdAndIdempotencyKey(merchantId, key)
                .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    private List<MerchantOrderResponse> findBulk(Long merchantId, String key, int size) {
        List<MerchantOrder> existing = orderRepository
                .findByMerchantIdAndIdempotencyKeyStartingWithOrderByIdAsc(merchantId, bulkPrefix(key));
        if (existing.isEmpty()) {
            throw new NotFoundException("Order not found");
        }
        if (existing.size() != size) {
            throw new ConflictException("Idempotency-Key already used with a different batch size");
        }
        return toResponses(existing);
    }

    private Page<MerchantOrderResponse> toResponsePage(Page<MerchantOrder> orders) {
        Map<Long, Parcel> parcelsById = loadParcelsById(orders.getContent());
        return orders.map(order -> toResponse(order, parcelsById));
    }

    private List<MerchantOrderResponse> toResponses(List<MerchantOrder> orders) {
        Map<Long, Parcel> parcelsById = loadParcelsById(orders);
        return orders.stream().map(order -> toResponse(order, parcelsById)).toList();
    }

    private Map<Long, Parcel> loadParcelsById(List<MerchantOrder> orders) {
        List<Long> parcelIds = orders.stream()
                .map(MerchantOrder::getParcelId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (parcelIds.isEmpty()) {
            return Map.of();
        }
        return parcelRepository.findAllById(parcelIds).stream()
                .collect(Collectors.toMap(Parcel::getId, Function.identity()));
    }

    private MerchantOrderResponse toResponse(MerchantOrder order, Map<Long, Parcel> parcelsById) {
        Parcel parcel = parcelsById.get(order.getParcelId());
        if (parcel == null) {
            throw new NotFoundException("Order parcel not found");
        }
        return mapper.toResponse(order, parcel);
    }

    private MerchantOrderResponse toResponse(MerchantOrder order) {
        Parcel parcel = parcelRepository.findById(order.getParcelId())
                .orElseThrow(() -> new NotFoundException("Order parcel not found"));
        return mapper.toResponse(order, parcel);
    }

    private static String escapeLikePattern(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    static String bulkPrefix(String key) {
        return "bulk:" + key.length() + ":" + key + ":";
    }

    static String bulkKey(String key, int index) {
        return bulkPrefix(key) + index;
    }

    public record CreationResult(MerchantOrderResponse response, boolean created) {}
    public record BulkCreationResult(List<MerchantOrderResponse> responses, boolean created) {}
    public record DailyOrderCount(LocalDate date, long count) {}
}
