package makeus.cmc.malmo.application.exception;

public class InvalidWebOAuthRequestException extends RuntimeException {

    public InvalidWebOAuthRequestException(String message) {
        super(message);
    }

    public InvalidWebOAuthRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
