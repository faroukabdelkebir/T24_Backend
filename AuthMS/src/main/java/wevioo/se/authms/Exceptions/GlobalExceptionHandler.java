package wevioo.se.authms.Exceptions;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE = "https://platform.bank.com/errors/";


    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication Failed",
                "Invalid username or password", "authentication-failed", req);
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex, WebRequest req) {
        return build(HttpStatus.FORBIDDEN, "Account Disabled",
                "Your account is disabled. Contact your administrator.", "account-disabled", req);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid Token", ex.getMessage(), "invalid-token", req);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ProblemDetail handleTokenExpired(TokenExpiredException ex, WebRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Token Expired", ex.getMessage(), "token-expired", req);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), "not-found", req);
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidation(ValidationException ex, WebRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Validation Error",
                ex.getMessage(), "validation-error", req);
    }


    @ExceptionHandler(UserServiceException.class)
    public ProblemDetail handleUserService(UserServiceException ex, WebRequest req) {
        log.error("UserMS unavailable: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                ex.getMessage(), "service-unavailable", req);
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));

        ProblemDetail problem = build(HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation Failed", "One or more fields are invalid.", "validation-error", req);
        problem.setProperty("fieldErrors", fieldErrors);

        return ResponseEntity.unprocessableEntity().body(problem);
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAll(Exception ex, WebRequest req) {
        log.error("Unhandled exception [{}]: {}", req.getDescription(false), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please contact support.", "internal-error", req);
    }

    // Builder

    private ProblemDetail build(HttpStatus status, String title,
                                String detail, String type, WebRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create(BASE + type));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setInstance(URI.create(req.getDescription(false).replace("uri=", "")));
        return pd;
    }
}

