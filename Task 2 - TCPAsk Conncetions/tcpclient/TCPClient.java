package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    public TCPClient() {
    
    }

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        Socket socket = new Socket(hostname, port);
        ByteArrayOutputStream response = new ByteArrayOutputStream();

        // Contact server
        socket.getOutputStream().write(toServerBytes);

        InputStream inputStream = socket.getInputStream();
        byte[] serverResponse = new byte[512]; // Moved inside the loop
        int length;

        while ((length = inputStream.read(serverResponse)) != -1) {
            response.write(serverResponse, 0, length);
        }

        socket.close();

        return response.toByteArray();
    }
}
