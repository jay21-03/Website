package com.bautruc.ecommerce.catalog.application;

import java.util.Locale;

import com.bautruc.ecommerce.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class ImageSignatureValidator {
    public ValidatedImage validate(byte[] content, String declaredContentType) {
        if (content == null || content.length == 0) {
            throw new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_EMPTY, "Image file is empty.");
        }
        String actualType;
        String extension;
        if (isJpeg(content)) {
            actualType = "image/jpeg";
            extension = "jpg";
        } else if (isPng(content)) {
            actualType = "image/png";
            extension = "png";
        } else if (isWebp(content)) {
            actualType = "image/webp";
            extension = "webp";
        } else {
            throw new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_SIGNATURE_INVALID,
                    "Image content is not a supported JPEG, PNG, or WebP file.");
        }
        String declared = declaredContentType == null ? "" : declaredContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!actualType.equals(declared)) {
            throw new BusinessException(CatalogErrorCodes.PRODUCT_IMAGE_TYPE_UNSUPPORTED,
                    "Declared image type does not match file content.");
        }
        return new ValidatedImage(content, actualType, extension);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && unsigned(bytes[0]) == 0xff && unsigned(bytes[1]) == 0xd8 && unsigned(bytes[2]) == 0xff;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if (unsigned(bytes[i]) != signature[i]) return false;
        return true;
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private int unsigned(byte value) { return value & 0xff; }
}
