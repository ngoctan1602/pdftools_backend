package com.tandev.pdftools.controller;

import com.tandev.pdftools.service.SplitPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/pdf/split")
@RequiredArgsConstructor
public class SplitPDF {
    private final SplitPdfService splitPdfService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> split(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ranges") String ranges) throws IOException {
        byte[] result = splitPdfService.splitByRange(file, ranges);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("split.pdf").build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(result);
    }

    @PostMapping(value = "/all", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> splitAll(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] zipBytes = splitPdfService.splitAll(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("split-pages.zip").build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipBytes);
    }
}
