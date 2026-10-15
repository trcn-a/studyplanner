package ua.studyplanner.common.controllers;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ua.studyplanner.common.exception.BusinessRuleException;
import ua.studyplanner.common.exception.ResourceNotFoundException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    ResponseEntity<?> credentials(Exception e) {return ResponseEntity.status(401).body(Map.of("message","Неправильна пошта або пароль"));}

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> notFound(ResourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<?> businessRule(BusinessRuleException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                "message",
                                "Перевірте обов’язкові поля, довжину тексту й додатну оцінку часу",
                                "fields",
                                e.getBindingResult().getFieldErrors().stream()
                                        .map(f -> f.getField())
                                        .distinct()
                                        .toList()));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<?> malformed(Exception e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Некоректний формат даних запиту"));
    }
}
