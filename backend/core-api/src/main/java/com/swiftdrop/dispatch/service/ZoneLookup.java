package com.swiftdrop.dispatch.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Reads the zones table (owned by M4) with small native queries, so the dispatch module
 * does not depend on M4's classes. One zone = one district.
 */
@Component
@RequiredArgsConstructor
public class ZoneLookup {

    /** One zone as shown in dropdowns. */
    public record ZoneOption(Long id, String name, String district) {}

    private final EntityManager entityManager;

    public List<ZoneOption> listAll() {
        List<?> rows = entityManager
                .createNativeQuery("select id, name, district from zones order by name")
                .getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] cols = (Object[]) row;
                    return new ZoneOption(((Number) cols[0]).longValue(),
                            String.valueOf(cols[1]), String.valueOf(cols[2]));
                })
                .toList();
    }

    public boolean exists(Long zoneId) {
        if (zoneId == null) {
            return false;
        }
        List<?> rows = entityManager
                .createNativeQuery("select id from zones where id = :id")
                .setParameter("id", zoneId)
                .getResultList();
        return !rows.isEmpty();
    }

    public Optional<String> districtOf(Long zoneId) {
        if (zoneId == null) {
            return Optional.empty();
        }
        List<?> rows = entityManager
                .createNativeQuery("select district from zones where id = :id")
                .setParameter("id", zoneId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(String.valueOf(rows.get(0)));
    }

    public Optional<Long> zoneIdOfDistrict(String district) {
        if (district == null || district.isBlank()) {
            return Optional.empty();
        }
        List<?> rows = entityManager
                .createNativeQuery("select id from zones where lower(district) = lower(:district)")
                .setParameter("district", district.trim())
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
    }
}