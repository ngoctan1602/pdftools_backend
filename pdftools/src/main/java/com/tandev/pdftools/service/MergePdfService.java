package com.tandev.pdftools.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MergePdfService {
    private static final int MAX_FILES_PER_REQUEST = 20;

    private final FileValidationService fileValidationService;

    public byte[] merge(List<MultipartFile> files) throws IOException {
        if (files == null || files.size() < 2) {
            throw new IllegalArgumentException("Cần ít nhất 2 file PDF để gộp.");
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                    "Chỉ hỗ trợ gộp tối đa " + MAX_FILES_PER_REQUEST + " file mỗi lần.");
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        merger.setDestinationStream(outputStream);

        for (MultipartFile file : files) {
            fileValidationService.validatePdf(file);
            try (InputStream sourceStream = file.getInputStream()) {
                merger.addSource(new RandomAccessReadBuffer(sourceStream));
            }
        }
        merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
        return outputStream.toByteArray();
    }
}
