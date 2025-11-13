package imd.ufrn.br.lifecycle;

public interface Lifecycle {
    void start() throws Exception;
    void stop() throws Exception;
    boolean isRunning();
}
