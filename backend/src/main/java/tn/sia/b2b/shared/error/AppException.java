package tn.sia.b2b.shared.error;

import java.util.List;

public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final List<String> details;

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.details = List.of();
    }

    public AppException(ErrorCode code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    public ErrorCode getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
