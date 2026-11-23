package com.example.app.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    public record ErrorResponse(Error error) {
        public record Error(String code, String message) {
        }
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handle(ApiException e) {
        return body(e.getStatus(), e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception e) {
        log.error("처리하지 못한 오류", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했어요.");
    }

    // 스프링 MVC 기본 예외도 같은 오류 형식으로 응답한다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object ignored, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            return handleUnexpected(e);
        }
        ErrorResponse.Error error = switch (status.value()) {
            case 400 -> new ErrorResponse.Error("INVALID_REQUEST", invalidMessage(e));
            case 404 -> new ErrorResponse.Error("NOT_FOUND", "요청한 주소를 찾을 수 없어요.");
            case 405 -> new ErrorResponse.Error("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식이에요.");
            default -> new ErrorResponse.Error("INVALID_REQUEST", "요청을 처리할 수 없어요.");
        };
        return new ResponseEntity<>(new ErrorResponse(error), headers, status);
    }

    private String invalidMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException invalid && invalid.getFieldError() != null) {
            return invalid.getFieldError().getField() + ": " + invalid.getFieldError().getDefaultMessage();
        }
        return "요청 값을 확인해 주세요.";
    }

    private ResponseEntity<Object> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(new ErrorResponse.Error(code, message)));
    }
}
