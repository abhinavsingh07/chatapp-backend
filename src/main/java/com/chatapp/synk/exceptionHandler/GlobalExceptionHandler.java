package com.chatapp.synk.exceptionHandler;

import com.chatapp.synk.response.BeanValidationErrors;
import com.chatapp.synk.response.ConstraintValidationErrors;
import com.chatapp.synk.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final int HTTP_CODE_BAD_REQUEST = 400;
    private final int HTTP_CODE_INTERNAL_SERVER_ERROR = 500;

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException exception) {
        HttpStatus status = exception.getStatus() != null
                ? exception.getStatus()
                : HttpStatus.INTERNAL_SERVER_ERROR;

        logger.error("A ServiceException occurred: {}", exception.getMessage());

        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        status.value(),
                        status,
                        exception.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        logger.warn("Invalid token: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED,
                        ex.getMessage()));
    }

    @ExceptionHandler(Exception.class) // catches Runtime Exception as well
    public ResponseEntity<ErrorResponse> handleOtherExceptions(Exception ex) {
        logger.error("Exception occured: {}", ex.getMessage());
        ErrorResponse errResp = new ErrorResponse();
        errResp.setResponseCode(HTTP_CODE_INTERNAL_SERVER_ERROR);
        errResp.setErrorMessage(ex.getMessage());
        logger.error("An unexpected Exception occurred: {}", ex.getMessage());
        return new ResponseEntity<>(errResp, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> processFieldValidationException(final MethodArgumentNotValidException ex) {
        logger.warn("Validation failed: {} field(s)", ex.getBindingResult().getFieldErrorCount());

        List<BeanValidationErrors> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(i -> new BeanValidationErrors(i.getField(), i.getDefaultMessage(), i.getRejectedValue()))
                .collect(Collectors.toList());

        ErrorResponse<BeanValidationErrors> resp = new ErrorResponse();
        resp.setResponseCode(HTTP_CODE_BAD_REQUEST);
        resp.setError(HttpStatus.BAD_REQUEST);
        resp.setErrors(errors);

        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> onConstraintValidationException(ConstraintViolationException e) {
        logger.warn("Constraint violations detected: {}", e.getConstraintViolations().size());

        List<ConstraintValidationErrors> errors = new ArrayList<>();
        for (ConstraintViolation violation : e.getConstraintViolations()) {
            errors.add(new ConstraintValidationErrors(violation.getPropertyPath().toString(), violation.getMessage()));
        }

        ErrorResponse<ConstraintValidationErrors> resp = new ErrorResponse();
        resp.setResponseCode(HTTP_CODE_BAD_REQUEST);
        resp.setError(HttpStatus.BAD_REQUEST);
        resp.setErrors(errors);

        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }
}
