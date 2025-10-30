package imd.ufrn.br.remoting;

import imd.ufrn.br.broker.Broker;
import imd.ufrn.br.identification.LookupService;
import imd.ufrn.br.identification.ObjectId;
import imd.ufrn.br.exceptions.MarshallingException;
import imd.ufrn.br.exceptions.ObjectNotFoundException;
import imd.ufrn.br.exceptions.RemotingException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServerRequestHandler {

    private final Broker broker;
    private final JsonMarshaller marshaller;
    private final LookupService lookupService;
    private final ExecutorService threadPool;
    
    private DatagramSocket socket;
    private boolean running = false;
    private static final int MAX_PACKET_SIZE = 65507;

    public UDPServerRequestHandler(Broker broker, JsonMarshaller marshaller, LookupService lookupService, int threadPoolSize) {
        if (broker == null || marshaller == null || lookupService == null) {
            throw new IllegalArgumentException("Broker, Marshaller, and LookupService cannot be null.");
        }
        this.broker = broker;
        this.marshaller = marshaller;
        this.lookupService = lookupService;
        this.threadPool = Executors.newFixedThreadPool(Math.max(1, threadPoolSize));
        System.out.println("UDPServerRequestHandler: Created with " + threadPoolSize + " threads");
    }

    public void start(int port) throws SocketException {
        if (running) {
            return;
        }

        socket = new DatagramSocket(port);
        running = true;

        Thread serverThread = new Thread(this::handleRequests);
        serverThread.setDaemon(false);
        serverThread.start();

        System.out.println("UDPServerRequestHandler: UDP server started on port " + port);
    }

    private void handleRequests() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                String message = new String(data);
                
                if ("HEARTBEAT".equals(message)) {
                    sendHeartbeatResponse(clientAddress, clientPort);
                } else {
                    threadPool.submit(() -> processRequest(message, clientAddress, clientPort));
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("UDPServerRequestHandler: Error receiving packet - " + e.getMessage());
                }
            }
        }
    }

    private void sendHeartbeatResponse(InetAddress clientAddress, int clientPort) {
        try {
            String response = "HEARTBEAT_ACK";
            DatagramPacket responsePacket = new DatagramPacket(
                response.getBytes(), response.length(),
                clientAddress, clientPort
            );
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("UDPServerRequestHandler: Error sending heartbeat response - " + e.getMessage());
        }
    }

    private void processRequest(String message, InetAddress clientAddress, int clientPort) {
        try {
            String[] parts = message.split("\\|", 3);
            if (parts.length != 3) {
                sendErrorResponse("Invalid request format. Expected: OBJECT_NAME|METHOD_NAME|JSON_PARAMS", 
                                clientAddress, clientPort);
                return;
            }

            String objectName = parts[0];
            String methodName = parts[1];
            String jsonParams = parts[2];

            System.out.println("UDPServerRequestHandler: Processing request for " + 
                             objectName + "#" + methodName + " with params: " + jsonParams);

            ObjectId objectId = new ObjectId(objectName);
            Object targetObject = lookupService.findObject(objectId);

            // Use the same robust method resolution as Invoker
            Class<?>[] paramTypes = findMethodParameterTypes(targetObject.getClass(), methodName, jsonParams);
            Object[] params = marshaller.unmarshalParameters(jsonParams, paramTypes);

            Object result = broker.processRequest(objectId, methodName, params);
            String jsonResponse = marshaller.marshal(result);

            sendSuccessResponse(jsonResponse, clientAddress, clientPort);

        } catch (ObjectNotFoundException e) {
            System.err.println("UDPServerRequestHandler: Object not found - " + e.getMessage());
            sendErrorResponse("Object not found: " + e.getMessage(), clientAddress, clientPort);
        } catch (NoSuchMethodException e) {
            System.err.println("UDPServerRequestHandler: Method not found - " + e.getMessage());
            sendErrorResponse("Method not found: " + e.getMessage(), clientAddress, clientPort);
        } catch (MarshallingException e) {
            System.err.println("UDPServerRequestHandler: Marshalling error - " + e.getMessage());
            sendErrorResponse("Error processing request data: " + e.getMessage(), clientAddress, clientPort);
        } catch (RemotingException e) {
            System.err.println("UDPServerRequestHandler: Remoting platform error - " + e.getMessage());
            sendErrorResponse("Platform error: " + e.getMessage(), clientAddress, clientPort);
        } catch (Throwable e) {
            System.err.println("UDPServerRequestHandler: Unhandled error - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage = e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage();
            }
            sendErrorResponse("Error during method execution: " + errorMessage, clientAddress, clientPort);
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

    private void sendSuccessResponse(String response, InetAddress clientAddress, int clientPort) {
        try {
            DatagramPacket responsePacket = new DatagramPacket(
                response.getBytes(), response.length(),
                clientAddress, clientPort
            );
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("UDPServerRequestHandler: Error sending success response - " + e.getMessage());
        }
    }

    private void sendErrorResponse(String errorMessage, InetAddress clientAddress, int clientPort) {
        try {
            String errorResponse = "ERROR: " + errorMessage;
            DatagramPacket responsePacket = new DatagramPacket(
                errorResponse.getBytes(), errorResponse.length(),
                clientAddress, clientPort
            );
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("UDPServerRequestHandler: Error sending error response - " + e.getMessage());
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("UDPServerRequestHandler: UDP server stopped");
    }
}
