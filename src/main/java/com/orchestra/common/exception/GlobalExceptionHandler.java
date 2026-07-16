package com.orchestra.common.exception;

import com.orchestra.common.dto.ApiErrorResponse;
import com.orchestra.job.domain.JobNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Merkezi hata yönetimi — Tüm controller exception'ları burada yakalanır.
 *
 * Her hata tipi için tutarlı ApiErrorResponse döner.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 404 — Kaynak bulunamadı ──────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                            HttpServletRequest request) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ── 404 — İş bulunamadı ──────────────────────────────
    // application katmanı HTTP'yi bilmez, sade bir JobNotFoundException fırlatır.
    // "Bunu 404'e çevirmek" bir WEB kararıdır ve burada, infrastructure'da yapılır.
    // Faz 3'te aynı use case'i HTTP'si olmayan worker çağırdığında bu satır
    // hiç devreye girmez — istisna orada başka türlü ele alınır. İşte ayrımın faydası.

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleJobNotFound(JobNotFoundException ex,
                                                              HttpServletRequest request) {
        log.warn("İş bulunamadı: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // ── 400 — Geçersiz istek ─────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex,
                                                              HttpServletRequest request) {
        log.warn("Geçersiz istek: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ── 400 — Validation hataları (@Valid) ────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        log.warn("Validation hatası: {} alan hatalı", fieldErrors.size());

        ApiErrorResponse body = ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "İstek doğrulama hatası",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── 400 — Constraint violation ───────────────────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                       HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // ── 409 — Conflict ───────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex,
                                                            HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    // ── 500 — Beklenmeyen hata (fallback) ────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex,
                                                           HttpServletRequest request) {
        // ERROR seviyesinde logla — beklenmeyen her hata buraya düşer
        log.error("Beklenmeyen hata: {} | Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.", request);
    }

    // ── Helper ───────────────────────────────────────────

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
                                                            HttpServletRequest request) {
        ApiErrorResponse body = ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private ApiErrorResponse.FieldError mapFieldError(FieldError fieldError) {
        return new ApiErrorResponse.FieldError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }
}
