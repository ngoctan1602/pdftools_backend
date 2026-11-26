package com.tandev.pdftools.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MergePdfServiceTest {

    @Mock
    private FileValidationService fileValidationService;

    @InjectMocks
    private MergePdfService mergePdfService;

    @Test
    void merge_nullOrLessThanTwoFiles_throwsIllegalArgument() throws IOException {
        MultipartFile single = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(1));

        assertThatThrownBy(() -> mergePdfService.merge(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> mergePdfService.merge(List.of(single)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void merge_moreThan20Files_throwsIllegalArgument() throws IOException {
        byte[] onePage = createPdf(1);
        List<MultipartFile> tooMany = IntStream.range(0, 21)
                .<MultipartFile>mapToObj(i -> new MockMultipartFile("files", i + ".pdf", "application/pdf", onePage))
                .toList();

        assertThatThrownBy(() -> mergePdfService.merge(tooMany))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void merge_validPdfs_validatesEachAndMergesPages() throws IOException {
        MultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(2));
        MultipartFile file2 = new MockMultipartFile("files", "b.pdf", "application/pdf", createPdf(3));

        byte[] merged = mergePdfService.merge(List.of(file1, file2));

        verify(fileValidationService, times(1)).validatePdf(file1);
        verify(fileValidationService, times(1)).validatePdf(file2);
        try (PDDocument doc = Loader.loadPDF(merged)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(5);
        }
    }

    @Test
    void merge_encryptedPdf_throwsInvalidPassword() throws IOException {
        MultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(1));
        MultipartFile locked = new MockMultipartFile("files", "locked.pdf", "application/pdf", createEncryptedPdf("123"));

        assertThatThrownBy(() -> mergePdfService.merge(List.of(file1, locked)))
                .isInstanceOf(InvalidPasswordException.class);
    }

    private byte[] createPdf(int pages) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEncryptedPdf(String password) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.protect(new StandardProtectionPolicy(password, password, new AccessPermission()));
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
