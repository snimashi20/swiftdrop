package com.swiftdrop.dispatch.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneLookupTest {

    @Mock EntityManager entityManager;
    @Mock Query query;
    @InjectMocks ZoneLookup zoneLookup;

    private void queryReturns(List<?> rows) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        doReturn(rows).when(query).getResultList();
    }

    @Test
    void exists_isTrue_whenTheZoneIsFound() {
        queryReturns(List.of(3L));

        assertTrue(zoneLookup.exists(3L));
    }

    @Test
    void exists_isFalse_whenTheZoneIsMissing() {
        queryReturns(List.of());

        assertFalse(zoneLookup.exists(99L));
    }

    @Test
    void exists_isFalse_withoutAnIdAndWithoutAskingTheDatabase() {
        assertFalse(zoneLookup.exists(null));

        verifyNoInteractions(entityManager);
    }

    @Test
    void districtOf_returnsTheDistrict() {
        queryReturns(List.of("Colombo"));

        assertEquals("Colombo", zoneLookup.districtOf(1L).orElseThrow());
    }

    @Test
    void districtOf_isEmpty_forAnUnknownOrMissingZone() {
        queryReturns(List.of());

        assertTrue(zoneLookup.districtOf(99L).isEmpty());
        assertTrue(zoneLookup.districtOf(null).isEmpty());
    }

    @Test
    void zoneIdOfDistrict_returnsTheZoneId() {
        queryReturns(List.of(5L));

        assertEquals(5L, zoneLookup.zoneIdOfDistrict(" colombo ").orElseThrow());
    }

    @Test
    void zoneIdOfDistrict_isEmpty_forBlankOrUnknownDistricts() {
        assertTrue(zoneLookup.zoneIdOfDistrict(null).isEmpty());
        assertTrue(zoneLookup.zoneIdOfDistrict("  ").isEmpty());
        verifyNoInteractions(entityManager);
    }

    @Test
    void zoneIdOfDistrict_isEmpty_whenNoZoneCoversTheDistrict() {
        queryReturns(List.of());

        assertTrue(zoneLookup.zoneIdOfDistrict("Atlantis").isEmpty());
    }

    @Test
    void listAll_mapsEveryRow() {
        queryReturns(Collections.singletonList(new Object[] {1L, "Colombo Zone", "Colombo"}));

        List<ZoneLookup.ZoneOption> zones = zoneLookup.listAll();

        assertEquals(1, zones.size());
        assertEquals(1L, zones.get(0).id());
        assertEquals("Colombo Zone", zones.get(0).name());
        assertEquals("Colombo", zones.get(0).district());
    }
}