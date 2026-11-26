package com.tandev.pdftools.exception;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.tandev.pdftools.dto.ApiErrorResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Translates validation, upload, and PDF processing exceptions into structured HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(400, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return buildError(400, "Bad Request",
                "Thiếu dữ liệu tải lên ở trường '" + ex.getRequestPartName() + "'.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return buildError(400, "Bad Request",
                "Thiếu tham số bắt buộc '" + ex.getParameterName() + "'.");
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        log.warn("Encrypted PDF rejected: {}", ex.getMessage());
        return buildError(400, "Encrypted PDF",
                "Có file PDF đang được đặt mật khẩu. Vui lòng mở khóa trước khi xử lý.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildError(413, "Payload Too Large",
                "Dung lượng file vượt quá giới hạn cho phép (tối đa 20MB/file, 100MB/lần gửi).");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleIOException(IOException ex) {
        log.error("PDF I/O error", ex);
        return buildError(422, "Unprocessable PDF",
                "Không thể đọc hoặc xử lý file PDF. File có thể bị hỏng cấu trúc.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server error", ex);
        return buildError(500, "Internal Server Error",
                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
    }

    private ResponseEntity<ApiErrorResponse> buildError(int status, String error, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status, error, message));
    }
}
