package com.tandev.pdftools.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tandev.pdftools.service.MergePdfService;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/pdf/merge")
@RequiredArgsConstructor
public class MergePDF {
    private final MergePdfService mergePdfService;

    @PostMapping()
    public ResponseEntity<byte[]> action(@RequestParam("files") List<MultipartFile> files) throws IOException {
        byte[] mergedPdf = mergePdfService.merge(files);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename("merged.pdf").build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(mergedPdf);
    }

}
