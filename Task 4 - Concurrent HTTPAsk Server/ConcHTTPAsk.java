import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

public class ConcHTTPAsk {
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
        } catch (Exception e) {
            System.err.println("Could not listen on port: " + port);
            System.err.println("Closing server...");
            return;
        }

        System.out.println("Server is running on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected");
            MyRunnable myRunnable = new MyRunnable(clientSocket, port);
            new Thread(myRunnable).start();
        }
    }
}