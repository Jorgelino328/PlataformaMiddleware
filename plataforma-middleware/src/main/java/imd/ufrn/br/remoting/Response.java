package imd.ufrn.br.remoting;

public class Response {
    private Object result;
    private Exception error;

    public Response(Object result, Exception error) {
        this.result = result;
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public Exception getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    public String getErrorMessage() {
        return hasError() ? error.getMessage() : null;
    }
}
