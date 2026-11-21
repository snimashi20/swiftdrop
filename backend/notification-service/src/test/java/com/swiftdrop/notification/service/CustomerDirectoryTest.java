package com.swiftdrop.notification.service;

import com.swiftdrop.notification.service.CustomerDirectory.CustomerContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The read-only lookup of the customer's contact details in core-api's users table. */
@ExtendWith(MockitoExtension.class)
class CustomerDirectoryTest {

    @Mock JdbcTemplate jdbc;

    @InjectMocks CustomerDirectory directory;

    @Test
    @DisplayName("an existing customer is returned, looked up by id in the users table")
    void existingCustomerIsReturned() {
        CustomerContact contact = new CustomerContact("sithmi@example.com", "Sithmi", "0771234567");
        givenLookupOf(7L, List.of(contact));

        Optional<CustomerContact> found = directory.findById(7L);

        assertThat(found).contains(contact);

        ArgumentCaptor<String> sql = ArgumentCaptor.captor();
        verify(jdbc).query(sql.capture(), ArgumentMatchers.<RowMapper<CustomerContact>>any(), eq(7L));
        assertThat(sql.getValue())
                .contains("SELECT email, full_name, phone_number")
                .contains("FROM users WHERE id = ?");
    }

    @Test
    @DisplayName("an unknown customer id is an empty Optional, not an exception")
    void unknownCustomerIsEmpty() {
        givenLookupOf(404L, List.of());

        assertThat(directory.findById(404L)).isEmpty();
    }

    @Test
    @DisplayName("if the lookup ever returns more than one row, the first one is used")
    void onlyTheFirstRowIsUsed() {
        CustomerContact first = new CustomerContact("first@example.com", "First", "0771111111");
        CustomerContact second = new CustomerContact("second@example.com", "Second", "0772222222");
        givenLookupOf(7L, List.of(first, second));

        assertThat(directory.findById(7L)).contains(first);
    }

    @Test
    @DisplayName("the row mapper reads e-mail, full name and phone number out of the result set")
    void rowMapperReadsEveryColumn() throws Exception {
        givenLookupOf(7L, List.of());
        directory.findById(7L);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("sithmi@example.com");
        when(rs.getString("full_name")).thenReturn("Sithmi");
        when(rs.getString("phone_number")).thenReturn("0771234567");

        CustomerContact mapped = capturedRowMapper().mapRow(rs, 0);

        assertThat(mapped).isNotNull();
        assertThat(mapped.email()).isEqualTo("sithmi@example.com");
        assertThat(mapped.fullName()).isEqualTo("Sithmi");
        assertThat(mapped.phoneNumber()).isEqualTo("0771234567");
    }

    @Test
    @DisplayName("a customer with no phone number on file maps to a null phone number")
    void rowMapperHandlesMissingPhoneNumber() throws Exception {
        givenLookupOf(7L, List.of());
        directory.findById(7L);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("email")).thenReturn("sithmi@example.com");
        when(rs.getString("full_name")).thenReturn("Sithmi");
        when(rs.getString("phone_number")).thenReturn(null);

        CustomerContact mapped = capturedRowMapper().mapRow(rs, 0);

        assertThat(mapped).isNotNull();
        assertThat(mapped.phoneNumber()).isNull();
        assertThat(mapped.email()).isEqualTo("sithmi@example.com");
    }

    // ----- helpers -----

    private void givenLookupOf(Long customerId, List<CustomerContact> rows) {
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<CustomerContact>>any(), eq(customerId)))
                .thenReturn(rows);
    }

    private RowMapper<CustomerContact> capturedRowMapper() {
        ArgumentCaptor<RowMapper<CustomerContact>> mapper = ArgumentCaptor.captor();
        verify(jdbc).query(anyString(), mapper.capture(), eq(7L));
        return mapper.getValue();
    }
}
