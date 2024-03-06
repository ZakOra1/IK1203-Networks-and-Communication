import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import tcpclient.TCPClient;

public class HTTPAsk {
    private static final String HTTP_OK = "HTTP/1.1 200 OK\r\n\r\n";
    private static final String HTTP_BAD_REQUEST = "HTTP/1.1 400 Bad Request\r\n\r\n";
    private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found\r\n\r\n";
    private static final String HTTP_NOT_SUPPORTED = "HTTP/1.1 501 Not Implemented\r\n\r\n";
    
    public static void main(String[] args) throws IOException { 
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number");
            System.err.println("Closing server...");
            return;
        }

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        }
        catch (Exception e) {
            System.err.println("Could not listen on port: " + port);
            System.err.println("Closing server...");
            return;
        }

        System.out.println("Server is running on port " + port);

        while (true) {
            System.out.println("Waiting for client to connect");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected");

            // Read all the input from the client
            InputStream input = clientSocket.getInputStream();
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = 0;

            // Print the input from the client
            while ((bytesRead = input.read(buffer)) != -1) {
                request.write(buffer, 0, bytesRead);
                String requeStr = request.toString();
                if (requeStr.contains("\r\n\r\n")) {
                    break;
                }
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
            OutputStream output = clientSocket.getOutputStream();
            byte[] response = null;

            // Only process GET requests
            if (!method.equals("GET")) {
                System.out.println("Unsupported method: " + method);
                output.write(HTTP_NOT_SUPPORTED.getBytes());
                clientSocket.close();
                continue;
            }

            // Parse the URI
            URL url = new URL("http", "localhost", uri);
            String query = url.getQuery();

            System.out.println("url.getQuery(): " + query);
            System.out.println("url.getPath(): " + url.getPath());

            // Check if the path is "/ask"
            if (!url.getPath().equals("/ask")) {
                System.err.println("Unsupported path: " + url.getPath());
                output.write(HTTP_NOT_FOUND.getBytes());
                clientSocket.close();
                continue;
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
                output.write(HTTP_NOT_FOUND.getBytes());
                clientSocket.close();
                continue;
            }
            
            String serverResponse = "";
            System.out.println("TCPClient created");

            // Check if hostname and port are valid
            if(hostname == null || port == 0) {
                System.out.println("Invalid hostname or port number");
                output.write(HTTP_BAD_REQUEST.getBytes());
                clientSocket.close();
                continue;
            }

            // Send the request to the server
            try {
                response = tcpClient.askServer(hostname, port, string);
            } catch (Exception e) {
                System.err.println("tcpClient askServer Error: " + e.getMessage());
                output.write(HTTP_NOT_FOUND.getBytes());
                clientSocket.close();
                continue;
            }

            System.out.println("Server responded");
            serverResponse = HTTP_OK + new String(response);

            // server output
            response = serverResponse.getBytes();
            System.out.println("Sending response to client");
            output.write(response);
            clientSocket.close();
        }
    }
}