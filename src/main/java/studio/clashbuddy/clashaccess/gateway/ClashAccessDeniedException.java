package studio.clashbuddy.clashaccess.gateway;

public class ClashAccessDeniedException extends RuntimeException {
    private final int code;

    public ClashAccessDeniedException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}