package makeus.cmc.malmo.application.exception;

public class PartnerProfileAlreadyExistsException extends IllegalArgumentException {
    public PartnerProfileAlreadyExistsException(String message) {
        super(message);
    }
}
