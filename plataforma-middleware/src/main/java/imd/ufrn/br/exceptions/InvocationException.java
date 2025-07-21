package imd.ufrn.br.exceptions;

public class InvocationException extends RuntimeException {

    public InvocationException() {
        super("An error occurred during method invocation.");
    }

    public InvocationException(String message) {
        super(message);
    }

    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvocationException(Throwable cause) {
        super(cause);
    }
}