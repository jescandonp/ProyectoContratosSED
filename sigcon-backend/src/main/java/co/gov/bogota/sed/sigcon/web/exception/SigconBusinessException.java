package co.gov.bogota.sed.sigcon.web.exception;

import org.springframework.http.HttpStatus;

public class SigconBusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public SigconBusinessException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
