package makeus.cmc.malmo.domain.exception;

public class AlreadyCoupledMemberException extends RuntimeException {
    public AlreadyCoupledMemberException(String message) {
        super(message);
    }
}
