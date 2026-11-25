package com.tandev.pdftools.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared file validation using Apache Tika magic-byte inspection.
 */
@Service
public class FileValidationService {

    private static final Set<String> PDF_MIME_TYPES = Set.of("application/pdf");
    private static final Set<String> IMAGE_MIME_TYPES = Set.of("image/jpeg", "image/png");

    private final Tika tika = new Tika();

    public void validatePdf(MultipartFile file) throws IOException {
        validateMimeType(file, PDF_MIME_TYPES, "File không phải định dạng PDF hợp lệ");
    }

    public void validateImage(MultipartFile file) throws IOException {
        validateMimeType(file, IMAGE_MIME_TYPES, "File phải là ảnh JPG hoặc PNG hợp lệ");
    }

    private void validateMimeType(MultipartFile file, Set<String> allowedTypes, String invalidMessage)
            throws IOException {
        if (file == null || file.isEmpty()) {
            String filename = file != null ? file.getOriginalFilename() : "unknown";
            throw new IllegalArgumentException("Không được gửi file rỗng: " + filename);
        }

        try (InputStream detectStream = file.getInputStream()) {
            String mimeType = tika.detect(detectStream, file.getOriginalFilename());
            if (mimeType == null || !allowedTypes.contains(mimeType.toLowerCase())) {
                throw new IllegalArgumentException(invalidMessage + ": " + file.getOriginalFilename());
            }
        }
    }
}
