package imd.ufrn.br.remoting;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    
    private final String serverHost;
    private final int serverPort;
    private static final int MAX_PACKET_SIZE = 65507;
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    public UDPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String sendRequest(String objectName, String methodName, String jsonParams) throws IOException {
        return sendRequest(objectName, methodName, jsonParams, DEFAULT_TIMEOUT_MS);
    }

    public String sendRequest(String objectName, String methodName, String jsonParams, int timeoutMs) throws IOException {
        String message = objectName + "|" + methodName + "|" + jsonParams;
        byte[] sendData = message.getBytes();
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);
            
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, address, serverPort
            );
            socket.send(sendPacket);
            
            byte[] receiveData = new byte[MAX_PACKET_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            socket.receive(receivePacket);
            
            return new String(receivePacket.getData(), 0, receivePacket.getLength());
        }
    }

    public boolean sendHeartbeat() throws IOException {
        return sendHeartbeat(DEFAULT_TIMEOUT_MS);
    }

    public boolean sendHeartbeat(int timeoutMs) throws IOException {
        String message = "HEARTBEAT";
        byte[] sendData = message.getBytes();
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(timeoutMs);
            
            InetAddress address = InetAddress.getByName(serverHost);
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, address, serverPort
            );
            socket.send(sendPacket);
            
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            
            return "HEARTBEAT_ACK".equals(response);
        }
    }
}
