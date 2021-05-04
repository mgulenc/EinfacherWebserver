
public enum Status {
    OK(200, "0K"), FORBIDDEN(403, "forbidden"), NOT_FOUND(404, "Not found"),
    NOT_IMPLEMENTED(501, "Not implemented");

    private int code;
    private String message;

    Status(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

