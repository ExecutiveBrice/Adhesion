package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.common.exception.InsufficientStockException;
import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.exception.ProductNotOrderableException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(assignableTypes = {ShopController.class, ShopAdminController.class})
public class ShopApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Requête invalide", "Certains champs sont invalides");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class})
    ProblemDetail malformedRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide", "Le corps de la requête ou ses en-têtes sont invalides");
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class, ArithmeticException.class})
    ProblemDetail badRequest(RuntimeException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "Ressource introuvable", exception.getMessage());
    }

    @ExceptionHandler({ProductNotOrderableException.class, InsufficientStockException.class})
    ProblemDetail unavailable(IllegalStateException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Article indisponible", exception.getMessage());
    }

    @ExceptionHandler({InvalidStatusTransitionException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Conflit", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail invalidState(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "État incompatible", exception.getMessage());
    }

    @ExceptionHandler(PaymentProviderException.class)
    ProblemDetail paymentProvider(PaymentProviderException exception) {
        HttpStatus status = exception.isRetryable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        return problem(status, "Service de paiement indisponible", "Le paiement ne peut pas être initialisé ou vérifié pour le moment");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? title : detail);
        problem.setTitle(title);
        return problem;
    }
}
