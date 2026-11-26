package com.tandev.pdftools.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SplitPdfServiceTest {

    @Mock
    private FileValidationService fileValidationService;

    @InjectMocks
    private SplitPdfService splitPdfService;

    @Test
    void splitByRange_singleAndOverlappingRanges_returnsSortedUniquePages() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(6));

        byte[] single = splitPdfService.splitByRange(file, "2");
        byte[] multi = splitPdfService.splitByRange(file, "4-5, 3-5, 1");

        verify(fileValidationService, org.mockito.Mockito.times(2)).validatePdf(file);
        try (PDDocument singleDoc = Loader.loadPDF(single);
                PDDocument multiDoc = Loader.loadPDF(multi)) {
            assertThat(singleDoc.getNumberOfPages()).isEqualTo(1);
            assertThat(multiDoc.getNumberOfPages()).isEqualTo(4);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "1-", "-3", "1-2-3", "1,,2", "1-2,", "5-2", "0", "6", "99999999999"})
    void splitByRange_invalidRanges_throwsIllegalArgument(String badRange) throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(5));

        assertThatThrownBy(() -> splitPdfService.splitByRange(file, badRange))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void splitAll_validPdf_returnsZipEntriesInOrder() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(3));

        byte[] zipBytes = splitPdfService.splitAll(file);

        verify(fileValidationService).validatePdf(file);
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                names.add(entry.getName());
                try (PDDocument pageDoc = Loader.loadPDF(zis.readAllBytes())) {
                    assertThat(pageDoc.getNumberOfPages()).isEqualTo(1);
                }
            }
        }
        assertThat(names).containsExactly("page-1.pdf", "page-2.pdf", "page-3.pdf");
    }

    @Test
    void splitAll_moreThan200Pages_throwsIllegalArgument() throws IOException {
        MockMultipartFile huge = new MockMultipartFile("file", "huge.pdf", "application/pdf", createPdf(201));

        assertThatThrownBy(() -> splitPdfService.splitAll(huge))
                .isInstanceOf(IllegalArgumentException.class);
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
}
