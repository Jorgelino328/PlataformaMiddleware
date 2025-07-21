package imd.ufrn.br.exceptions;

public class RemotingException extends RuntimeException {

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}