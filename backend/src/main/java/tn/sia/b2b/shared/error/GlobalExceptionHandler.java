package tn.sia.b2b.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("Application exception [{}] traceId={}: {}", ex.getCode(), traceId, ex.getMessage());
        ErrorResponse body = ErrorResponse.of(traceId, ex.getCode(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getCode().getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        List<String> details = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + " : " + fe.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .toList();
        log.debug("Validation failed traceId={}: {}", traceId, details);
        ErrorResponse body = ErrorResponse.of(traceId, ErrorCode.VALIDATION_FAILED, "La requête contient des champs invalides.", details);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("Unexpected error traceId={}", traceId, ex);
        ErrorResponse body = ErrorResponse.of(traceId, ErrorCode.INTERNAL_ERROR, "Une erreur inattendue s'est produite. Référence : " + traceId);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(body);
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader("X-Trace-Id");
        if (header != null && !header.isBlank()) return header;
        Object attr = request.getAttribute("traceId");
        if (attr != null) return attr.toString();
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
