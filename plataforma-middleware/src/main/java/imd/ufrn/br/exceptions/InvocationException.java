package imd.ufrn.br.exceptions;

/**
 * Exception thrown when an error occurs during method invocation.
 */
public class InvocationException extends RuntimeException {

    /**
     * Default constructor with a generic error message.
     */
    public InvocationException() {
        super("An error occurred during method invocation.");
    }

    /**
     * Constructor with a custom error message.
     *
     * @param message The custom error message.
     */
    public InvocationException(String message) {
        super(message);
    }

    /**
     * Constructor with a custom error message and a cause.
     *
     * @param message The custom error message.
     * @param cause   The cause of the exception.
     */
    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause The cause of the exception.
     */
    public InvocationException(Throwable cause) {
        super(cause);
    }
}