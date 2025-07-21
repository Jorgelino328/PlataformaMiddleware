package imd.ufrn.br.exceptions;

public class MarshallingException extends RemotingException {
    public MarshallingException(String message) {
        super(message);
    }

    public MarshallingException(String message, Throwable cause) {
        super(message, cause);
    }
}