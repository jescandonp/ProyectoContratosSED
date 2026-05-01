package co.gov.bogota.sed.sigcon.web.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SigconBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(SigconBusinessException exception) {
        ErrorResponse response = new ErrorResponse(
            exception.getErrorCode().name(),
            exception.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        ErrorResponse response = new ErrorResponse(
            "VALIDACION_FALLIDA",
            "La solicitud no cumple las validaciones requeridas",
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(response);
    }
}
