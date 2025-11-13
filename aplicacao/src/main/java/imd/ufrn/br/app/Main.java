package imd.ufrn.br.app;

import imd.ufrn.br.MiddlewarePlatform;
import imd.ufrn.br.extensions.Extension;

public class Main {

    public static void main(String[] args) {
        System.out.println("Iniciando aplicação middleware distribuído...");

        try {
            MiddlewarePlatform platform = new MiddlewarePlatform();
            platform.start(args); 

            // Register services
            platform.registerService(new CalculatorServiceImpl());

            // Register extensions
            platform.registerExtension(new Extension() {
                @Override
                public void onInvoke(String objectId, String methodName) {
                    System.out.println("[AppExtension] Invocação de " + objectId + "#" + methodName);
                }
            });

            System.out.println("Aplicação iniciada. Pressione Ctrl+C para sair.");
            
            // Wait for shutdown (shutdown hook is registered in MiddlewarePlatform)
            platform.awaitShutdown();

        } catch (Exception e) {
            System.err.println("Falha ao iniciar aplicação: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
