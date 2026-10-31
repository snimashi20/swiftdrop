package com.swiftdrop.merchant.exception;

import com.swiftdrop.common.exception.ForbiddenException;

public class MerchantNotApprovedException extends ForbiddenException {

    public MerchantNotApprovedException(String message) {
        super(message);
    }
}
