package imd.ufrn.br.remoting;

import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.exceptions.MarshallingException;
import imd.ufrn.br.exceptions.ObjectNotFoundException;
import imd.ufrn.br.exceptions.RemotingException;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServerRequestHandler {

    private final Broker broker;
    private final JsonMarshaller marshaller;
    private final LookupService lookupService;
    private ServerSocket serverSocket;
    private DatagramSocket heartbeatSocket;
    private final ExecutorService threadPool;
    private boolean isRunning = false;

    public TCPServerRequestHandler(Broker broker, JsonMarshaller marshaller, LookupService lookupService, int threadPoolSize) {
        if (broker == null || marshaller == null || lookupService == null) {
            throw new IllegalArgumentException("Broker, Marshaller, and LookupService cannot be null.");
        }
        this.broker = broker;
        this.marshaller = marshaller;
        this.lookupService = lookupService;
        this.threadPool = Executors.newFixedThreadPool(Math.max(1, threadPoolSize));
        System.out.println("TCPServerRequestHandler: Created with " + threadPoolSize + " threads");
    }

    public void start(int port) throws IOException {
        if (isRunning) {
            return;
        }
        
        serverSocket = new ServerSocket(port);
        heartbeatSocket = new DatagramSocket(port + 1000); // Use TCP port + 1000 for heartbeat UDP
        isRunning = true;
        
        // Start TCP server thread
        Thread serverThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleTCPRequest(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("TCPServerRequestHandler: Error accepting TCP connection - " + e.getMessage());
                    }
                }
            }
        });
        serverThread.start();
        
        // Start UDP heartbeat listener thread
        Thread heartbeatThread = new Thread(() -> {
            while (isRunning) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    heartbeatSocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if ("HEARTBEAT".equals(message)) {
                        // Send heartbeat response
                        String response = "HEARTBEAT_ACK";
                        byte[] responseData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                            responseData, responseData.length, packet.getAddress(), packet.getPort()
                        );
                        heartbeatSocket.send(responsePacket);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("TCPServerRequestHandler: Error handling heartbeat - " + e.getMessage());
                    }
                }
            }
        });
        heartbeatThread.start();
        
        System.out.println("TCPServerRequestHandler: TCP server started on port " + port);
        System.out.println("TCPServerRequestHandler: Heartbeat listener started on UDP port " + (port + 1000));
    }

    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("TCPServerRequestHandler: Error closing TCP server socket - " + e.getMessage());
        }
        
        try {
            if (heartbeatSocket != null && !heartbeatSocket.isClosed()) {
                heartbeatSocket.close();
            }
        } catch (Exception e) {
            System.err.println("TCPServerRequestHandler: Error closing heartbeat socket - " + e.getMessage());
        }
        
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("TCPServerRequestHandler: TCP server stopped");
    }

    private void handleTCPRequest(Socket clientSocket) {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            String request = reader.readLine();
            
            if (request != null && !request.isEmpty()) {
                System.out.println("TCPServerRequestHandler: Received request: " + request);
                
                String[] parts = request.split("\\|", 3);
                
                if (parts.length >= 3) {
                    String objectName = parts[0];
                    String methodName = parts[1];
                    String paramsJson = parts[2];
                    
                    try {
                        ObjectId objectId = new ObjectId(objectName);
                        Object targetObject = lookupService.findObject(objectId);
                        
                        Class<?>[] paramTypes = findMethodParameterTypes(targetObject.getClass(), methodName, paramsJson);
                        Object[] params = marshaller.unmarshalParameters(paramsJson, paramTypes);
                        
                        Object result = broker.processRequest(objectId, methodName, params);
                        String jsonResponse = marshaller.marshal(result);
                        
                        writer.println(jsonResponse);
                        
                    } catch (ObjectNotFoundException e) {
                        writer.println("ERROR: Object not found - " + e.getMessage());
                    } catch (MarshallingException e) {
                        writer.println("ERROR: Marshalling error - " + e.getMessage());
                    } catch (RemotingException e) {
                        writer.println("ERROR: Remoting error - " + e.getMessage());
                    } catch (Throwable e) {
                        writer.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                } else {
                    writer.println("ERROR: Invalid request format. Expected: OBJECT_NAME|METHOD_NAME|PARAMS_JSON");
                }
            } else {
                writer.println("ERROR: Empty request");
            }
            
        } catch (IOException e) {
            System.err.println("TCPServerRequestHandler: Error handling TCP request - " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("TCPServerRequestHandler: Error closing client socket - " + e.getMessage());
            }
        }
    }

    private Class<?>[] findMethodParameterTypes(Class<?> targetClass, String methodName, String paramsJson) throws MarshallingException {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object> paramsList = marshaller.unmarshal(paramsJson, java.util.List.class);
            int paramCount = paramsList != null ? paramsList.size() : 0;
            
            for (java.lang.reflect.Method method : targetClass.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
                    return method.getParameterTypes();
                }
            }
            
            throw new MarshallingException("Method " + methodName + " with " + paramCount + " parameters not found in class " + targetClass.getName(), null);
            
        } catch (Exception e) {
            if (e instanceof MarshallingException) {
                throw e;
            }
            throw new MarshallingException("Error finding method parameter types: " + e.getMessage(), e);
        }
    }
}
