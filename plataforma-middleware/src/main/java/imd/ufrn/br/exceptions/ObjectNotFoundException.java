package imd.ufrn.br.exceptions;

/**
 * Exception thrown when an object cannot be found in the lookup service.
 */
public class ObjectNotFoundException extends RuntimeException {

    /**
     * Default constructor with a generic error message.
     */
    public ObjectNotFoundException() {
        super("Object not found.");
    }

    /**
     * Constructor with a custom error message.
     *
     * @param message The custom error message.
     */
    public ObjectNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with a custom error message and a cause.
     *
     * @param message The custom error message.
     * @param cause   The cause of the exception.
     */
    public ObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause.
     *
     * @param cause The cause of the exception.
     */
    public ObjectNotFoundException(Throwable cause) {
        super(cause);
    }
}