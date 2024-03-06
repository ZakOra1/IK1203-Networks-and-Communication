import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import tcpclient.TCPClient;

public class MyRunnable implements Runnable {
    private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String HTTP_BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n\r\n";
    private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
    private static final String HTTP_NOT_SUPPORTED = "HTTP/1.1 501 Not Implemented\r\n\r\n";

    private Socket clientSocket;
    private int port;

    public MyRunnable(Socket clientSocket, int port) {
        this.clientSocket = clientSocket;
    }

    public void writeOutput(OutputStream output, byte[] response) {
        try {
            output.write(response);
        } catch (IOException e) {
            System.err.println("Error writing to output stream: " + e.getMessage());
        }
    }

    public void closeSocket() {
        try {
            this.clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        // Read all the input from the client
        InputStream input = null;
        ByteArrayOutputStream request = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead = 0;

        try {
            input = this.clientSocket.getInputStream();
            while ((bytesRead = input.read(buffer)) != -1) {
                request.write(buffer, 0, bytesRead);
                String requeStr = request.toString();
                if (requeStr.contains("\r\n\r\n")) {
                    break;
                }
            }
        } catch(IOException e) {
            System.err.println("Error getting input stream: " + e.getMessage());
            return;
        }

        System.out.println("Successfully read input from client");

        // Split the request into header and body parts
        String requestStr = request.toString();
        String[] parts = requestStr.split("\r\n\r\n", 2);
        String header = parts[0];

        // Extract the URI from the first line of the header
        String[] lines = header.split("\r\n");
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String uri = requestLine[1];
        String version = requestLine[2];

        System.out.println("Header read from Client: \n" + header);

        // Server output
        OutputStream output = null;
        try {
            output = clientSocket.getOutputStream();
        } catch (IOException e) { 
            System.err.println("Error getting output stream: " + e.getMessage());
            return;
        }
        byte[] response = null;

        // Only process GET requests
        if(!method.equals("GET")) {
            System.out.println("Unsupported method: " + method);
            writeOutput(output, HTTP_NOT_SUPPORTED.getBytes());
            closeSocket();
            return;
        }

        // Parse the URI
        URL url = null;
        try {
            url = new URL("http", "localhost", uri);
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL Exception: " + e.getMessage());   
        }
        
        String query = url.getQuery();

        System.out.println("url.getQuery(): " + query);
        System.out.println("url.getPath(): " + url.getPath());

        // Check if the path is "/ask"
        if (!url.getPath().equals("/ask")) {
            System.err.println("Unsupported path: " + url.getPath());
            writeOutput(output, HTTP_NOT_FOUND.getBytes());
            closeSocket();
            return;
        }

        // Split the query into parameters and store them in a map for easy access
        String[] parameters = query.split("&");
        Map<String, String> paramMap = new HashMap<>();
        for (String parameter : parameters) {
            String[] nameValue = parameter.split("=");
            paramMap.put(nameValue[0], nameValue[1]);
        }

        // Extract the required (lab instruction) parameters
        String hostname = paramMap.get("hostname");
        port = Integer.parseInt(paramMap.get("port"));
        byte[] string = paramMap.containsKey("string") ? paramMap.get("string").concat("\n").getBytes() : new byte[0];
        boolean shutdown = Boolean.parseBoolean(paramMap.get("shutdown"));
        int limit = paramMap.containsKey("limit") ? Integer.parseInt(paramMap.get("limit")) : Integer.MAX_VALUE;
        int timeout = paramMap.containsKey("timeout") ? Integer.parseInt(paramMap.get("timeout")) : 0;

        // Create TCPClient
        TCPClient tcpClient;
        try {
            tcpClient = new TCPClient(shutdown, timeout, limit);
        } catch (Exception e) {
            System.err.println("Failed to create TCPClient: " + e.getMessage());
            writeOutput(output, HTTP_NOT_FOUND.getBytes());
            closeSocket();
            return;
        }

        String serverResponse = "";
        System.out.println("TCPClient created");

        // Check if hostname and port are valid
        if (hostname == null || port == 0) {
            System.out.println("Invalid hostname or port number");
            writeOutput(output, HTTP_BAD_REQUEST.getBytes());
            closeSocket();
            return;
        }

        // Send the request to the server
        try {
            response = tcpClient.askServer(hostname, port, string);
        } catch (Exception err) {
            System.err.println("tcpClient askServer Error: " + err.getMessage());
            writeOutput(output, HTTP_NOT_FOUND.getBytes());
            closeSocket();
            return;
        }

        System.out.println("Server responded");
        serverResponse = HTTP_OK + new String(response);

        // server output
        response = serverResponse.getBytes();
        System.out.println("Sending response to client");
        writeOutput(output, response);
        closeSocket();
    }
}
