package com.tandev.pdftools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class PdfToolsIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void merge_validPdfs_returnsMergedPdfWithExpectedPageCount() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(2));
        MockMultipartFile file2 = new MockMultipartFile("files", "b.pdf", "application/pdf", createPdf(3));

        MvcResult result = mockMvc.perform(multipart("/pdf/merge").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        try (PDDocument merged = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            assertThat(merged.getNumberOfPages()).isEqualTo(5);
        }
    }

    @Test
    void merge_singleFile_returns400() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(1));

        mockMvc.perform(multipart("/pdf/merge").file(file1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void merge_fakePdf_returns400() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(1));
        MockMultipartFile fake = new MockMultipartFile(
                "files", "fake.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/pdf/merge").file(file1).file(fake))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void merge_encryptedPdf_returns400EncryptedPdf() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", createPdf(1));
        MockMultipartFile locked = new MockMultipartFile(
                "files", "locked.pdf", "application/pdf", createEncryptedPdf(1, "123456"));

        mockMvc.perform(multipart("/pdf/merge").file(file1).file(locked))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Encrypted PDF"));
    }

    @Test
    void splitByRange_overlappingUnsortedRanges_deduplicatesAndSorts() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(6));

        MvcResult result = mockMvc.perform(multipart("/pdf/split")
                        .file(file)
                        .param("ranges", "4-5,3-5,1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        try (PDDocument split = Loader.loadPDF(result.getResponse().getContentAsByteArray())) {
            assertThat(split.getNumberOfPages()).isEqualTo(4);
        }
    }

    @Test
    void splitByRange_invalidRangeOrMissingParam_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(5));

        mockMvc.perform(multipart("/pdf/split").file(file).param("ranges", "5-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(multipart("/pdf/split").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Thiếu tham số bắt buộc 'ranges'."));
    }

    @Test
    void splitAll_validPdf_returnsZipWithOnePdfPerPage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", createPdf(3));

        MvcResult result = mockMvc.perform(multipart("/pdf/split/all").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andReturn();

        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                byte[] pageBytes = zis.readAllBytes();
                try (PDDocument pageDoc = Loader.loadPDF(pageBytes)) {
                    assertThat(pageDoc.getNumberOfPages()).isEqualTo(1);
                }
            }
        }
        assertThat(entryNames).containsExactly("page-1.pdf", "page-2.pdf", "page-3.pdf");
    }

    private byte[] createPdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createEncryptedPdf(int pageCount, String password) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 0; i < pageCount; i++) {
                doc.addPage(new PDPage());
            }
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    password, password, new AccessPermission());
            doc.protect(policy);
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
