package imd.ufrn.br.exceptions;

/**
 * Base class for all custom exceptions related to the remoting framework.
 *
 * This exception (and its subclasses) indicates a problem encountered during
 * remote object interactions, such as issues with marshalling, object lookup,
 * or method invocation.
 *
 * It extends {@link RuntimeException}, making it an unchecked exception.
 * This is often preferred for framework-level exceptions where the calling
 * code might not be able to directly recover from the problem.
 */
public class RemotingException extends RuntimeException {

    /**
     * Constructs a new remoting exception with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public RemotingException(String message) {
        super(message);
    }

    /**
     * Constructs a new remoting exception with the specified detail message
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
    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}