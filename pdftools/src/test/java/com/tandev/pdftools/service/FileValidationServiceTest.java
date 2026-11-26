package com.tandev.pdftools.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidationServiceTest {

    private final FileValidationService service = new FileValidationService();

    @Test
    void validatePdf_validPdf_doesNotThrow() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "ok.pdf", "application/pdf", createPdfBytes());

        assertThatCode(() -> service.validatePdf(file)).doesNotThrowAnyException();
    }

    @Test
    void validatePdf_nullOrEmpty_throwsIllegalArgument() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.validatePdf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePdf(empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatePdf_nonPdfContentWithPdfExtension_throwsIllegalArgument() throws IOException {
        MockMultipartFile textAsPdf = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "hello world".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile pngAsPdf = new MockMultipartFile(
                "file", "img.pdf", "application/pdf", createImageBytes("png"));

        assertThatThrownBy(() -> service.validatePdf(textAsPdf))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.validatePdf(pngAsPdf))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateImage_validJpgAndPng_doesNotThrow() throws IOException {
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", createImageBytes("png"));
        MockMultipartFile jpg = new MockMultipartFile("file", "b.jpg", "image/jpeg", createImageBytes("jpg"));

        assertThatCode(() -> service.validateImage(png)).doesNotThrowAnyException();
        assertThatCode(() -> service.validateImage(jpg)).doesNotThrowAnyException();
    }

    @Test
    void validateImage_pdfDisguisedAsJpg_throwsIllegalArgument() throws IOException {
        MockMultipartFile pdfAsJpg = new MockMultipartFile(
                "file", "doc.jpg", "image/jpeg", createPdfBytes());

        assertThatThrownBy(() -> service.validateImage(pdfAsJpg))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] createPdfBytes() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createImageBytes(String format) throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, format, baos);
            return baos.toByteArray();
        }
    }
}
