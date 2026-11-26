package com.tandev.pdftools.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SplitPdfService {

    private static final Pattern RANGE_PART_PATTERN = Pattern.compile("^(\\d+)(?:-(\\d+))?$");
    private static final int MAX_PAGES_FOR_SPLIT_ALL = 200;
    private final FileValidationService fileValidationService;

    public byte[] splitByRange(MultipartFile file, String ranges) throws IOException {
        fileValidationService.validatePdf(file);
        if (ranges == null || ranges.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập khoảng trang cần tách (ví dụ: 1-3,5).");
        }
        try (InputStream sourceStream = file.getInputStream();
                PDDocument sourceDoc = Loader.loadPDF(new RandomAccessReadBuffer(sourceStream));
                PDDocument outputDoc = new PDDocument();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int totalPages = sourceDoc.getNumberOfPages();
            Set<Integer> pagesToExtract = parseRanges(ranges, totalPages);
            for (int pageNum : pagesToExtract) {
                outputDoc.importPage(sourceDoc.getPage(pageNum - 1));
            }
            outputDoc.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private Set<Integer> parseRanges(String ranges, int totalPages) {
        Set<Integer> result = new LinkedHashSet<>();
        for (String rawPart : ranges.split(",", -1)) {
            String part = rawPart.trim();
            Matcher matcher = RANGE_PART_PATTERN.matcher(part);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Khoảng trang không hợp lệ: '" + part + "'");
            }
            int start = validatePage(matcher.group(1), totalPages);
            int end = matcher.group(2) != null ? validatePage(matcher.group(2), totalPages) : start;
            if (start > end) {
                throw new IllegalArgumentException("Trang bắt đầu phải <= trang kết thúc: '" + part + "'");
            }
            IntStream.rangeClosed(start, end).forEach(result::add);
        }
        return result;
    }

    private int validatePage(String value, int totalPages) {
        try {
            int page = Integer.parseInt(value);
            if (page < 1 || page > totalPages) {
                throw new IllegalArgumentException(
                        "Số trang " + page + " nằm ngoài phạm vi (1-" + totalPages + ").");
            }
            return page;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Số trang quá lớn: '" + value + "'");
        }
    }

    public byte[] splitAll(MultipartFile file) throws IOException {
        fileValidationService.validatePdf(file);

        try (InputStream sourceStream = file.getInputStream();
                PDDocument sourceDoc = Loader.loadPDF(new RandomAccessReadBuffer(sourceStream));
                ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(zipBaos)) {

            int totalPages = sourceDoc.getNumberOfPages();
            if (totalPages > MAX_PAGES_FOR_SPLIT_ALL) {
                throw new IllegalArgumentException(
                        "Chỉ hỗ trợ tách từng trang tối đa " + MAX_PAGES_FOR_SPLIT_ALL + " trang.");
            }

            List<PDDocument> pages = new Splitter().split(sourceDoc);
            for (int i = 0; i < pages.size(); i++) {
                try (PDDocument pageDoc = pages.get(i)) {
                    zos.putNextEntry(new ZipEntry("page-" + (i + 1) + ".pdf"));
                    pageDoc.save(zos);
                    zos.closeEntry();
                }
            }

            zos.finish();
            return zipBaos.toByteArray();
        }
    }
}
