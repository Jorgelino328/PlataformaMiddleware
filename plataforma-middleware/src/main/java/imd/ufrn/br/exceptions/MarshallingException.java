package imd.ufrn.br.exceptions;

/**
 * Custom exception to indicate an error during the marshalling (serialization)
 * or unmarshalling (deserialization) of data.
 *
 * This exception is typically thrown by a Marshaller implementation when it
 * fails to convert data to/from its wire format (e.g., JSON, XML).
 */
public class MarshallingException extends RemotingException {
    /**
     * Constructs a new marshalling exception with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public MarshallingException(String message) {
        super(message);
    }

    /**
     * Constructs a new marshalling exception with the specified detail message
     * and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is
     * <em>not</em> automatically incorporated in this exception's detail
     * message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the
     *                {@link #getCause()} method). (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }
}