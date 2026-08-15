package com.bautruc.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bautruc.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class ImageSignatureValidatorTest {
    private final ImageSignatureValidator validator = new ImageSignatureValidator();

    @Test void recognizesSupportedSignatures() {
        assertThat(validator.validate(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}, "image/jpeg").extension()).isEqualTo("jpg");
        assertThat(validator.validate(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}, "image/png").extension()).isEqualTo("png");
        assertThat(validator.validate(new byte[]{'R','I','F','F',0,0,0,0,'W','E','B','P'}, "image/webp").extension()).isEqualTo("webp");
    }

    @Test void rejectsMismatchedDeclaredType() {
        assertThatThrownBy(() -> validator.validate(
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(CatalogErrorCodes.PRODUCT_IMAGE_TYPE_UNSUPPORTED));
    }

    @Test void rejectsUnknownSignature() {
        assertThatThrownBy(() -> validator.validate(new byte[]{1, 2, 3, 4}, "image/jpeg"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(CatalogErrorCodes.PRODUCT_IMAGE_SIGNATURE_INVALID));
    }
}
