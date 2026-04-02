package makeus.cmc.malmo.application.exception;

public class InvalidWeeklyAnalysisWeekException extends RuntimeException {
    public InvalidWeeklyAnalysisWeekException() {
        super("weekStartDate must be a Monday.");
    }
}
