package com.swiftdrop.parcel.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.swiftdrop.parcel.domain.DeliveryPayer;
import com.swiftdrop.parcel.domain.ParcelItemType;
import com.swiftdrop.parcel.domain.ParcelSize;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateParcelRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private ParcelAddressDto address() {
        ParcelAddressDto address = new ParcelAddressDto();
        address.setLine1("12 Galle Road");
        address.setCity("Colombo");
        address.setDistrict("Colombo");
        return address;
    }

    private CreateParcelRequest validRequest() {
        CreateParcelRequest request = new CreateParcelRequest();
        request.setPickup(address());
        request.setDropoff(address());
        request.setReceiverName("Nimal Perera");
        request.setReceiverPhone("0771234567");
        request.setSize(ParcelSize.M);
        request.setWeightKg(new BigDecimal("2.5"));
        return request;
    }

    private boolean hasViolationOn(Set<ConstraintViolation<CreateParcelRequest>> violations, String path) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(path));
    }

    @Test
    void validRequest_hasNoViolations() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void missingReceiverName_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setReceiverName(" ");

        assertTrue(hasViolationOn(validator.validate(request), "receiverName"));
    }

    @Test
    void zeroWeight_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setWeightKg(BigDecimal.ZERO);

        assertTrue(hasViolationOn(validator.validate(request), "weightKg"));
    }

    @Test
    void negativeWeight_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setWeightKg(new BigDecimal("-1"));

        assertTrue(hasViolationOn(validator.validate(request), "weightKg"));
    }

    @Test
    void tooHeavyWeight_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setWeightKg(new BigDecimal("10000"));

        assertTrue(hasViolationOn(validator.validate(request), "weightKg"));
    }

    @Test
    void missingSize_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setSize(null);

        assertTrue(hasViolationOn(validator.validate(request), "size"));
    }

    @Test
    void missingPickupAddress_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setPickup(null);

        assertTrue(hasViolationOn(validator.validate(request), "pickup"));
    }

    @Test
    void blankDropoffDistrict_isRejected() {
        CreateParcelRequest request = validRequest();
        request.getDropoff().setDistrict("");

        assertTrue(hasViolationOn(validator.validate(request), "dropoff.district"));
    }

    @Test
    void tooLongReceiverPhone_isRejected() {
        CreateParcelRequest request = validRequest();
        request.setReceiverPhone("0".repeat(21));

        Set<ConstraintViolation<CreateParcelRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertTrue(hasViolationOn(violations, "receiverPhone"));
    }

    @Test
    void phoneInInternationalFormat_isAccepted() {
        CreateParcelRequest request = validRequest();
        request.setReceiverPhone("+94771234567");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void phoneWithLettersOrWrongLength_isRejected() {
        for (String phone : new String[] {"abc", "12345", "077123456", "07712345678", "0000000000"}) {
            CreateParcelRequest request = validRequest();
            request.setReceiverPhone(phone);

            assertTrue(hasViolationOn(validator.validate(request), "receiverPhone"), phone);
        }
    }

    @Test
    void receiverNameWithDigitsOrSymbols_isRejected() {
        for (String name : new String[] {"hhhh123", "Nimal@Perera", "12345", "-Nimal"}) {
            CreateParcelRequest request = validRequest();
            request.setReceiverName(name);

            assertTrue(hasViolationOn(validator.validate(request), "receiverName"), name);
        }
    }

    @Test
    void receiverNameWithSpacesDotsAndApostrophes_isAccepted() {
        CreateParcelRequest request = validRequest();
        request.setReceiverName("K. D. O'Neil-Perera");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void receiverNameInSinhalaOrTamil_isAccepted() {
        for (String name : new String[] {"සුනිල් පෙරේරා", "கார்த்திக் குமார்"}) {
            CreateParcelRequest request = validRequest();
            request.setReceiverName(name);

            assertTrue(validator.validate(request).isEmpty(), name);
        }
    }

    @Test
    void itemTypeAndPayerAreOptional() {
        CreateParcelRequest request = validRequest();
        request.setItemType(ParcelItemType.FOOD);
        request.setDeliveryPaidBy(DeliveryPayer.RECEIVER);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals(ParcelItemType.FOOD, request.getItemType());
    }
}
