package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    
    public TCPClient() {
    
    }
    /*public byte[] askServer(String hostname, int port) throws IOException {
        
        //Open connection with server
        int length = 0;
        Socket socket = new Socket(hostname, port);
        byte[] serverResponse = new byte[512];
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        
        InputStream inputStream = socket.getInputStream();

        //Server responds
        while(socket.getInputStream().read() != -1) {
            length = inputStream.read(serverResponse);
            response.write(serverResponse, 0, length);
        }
        
        socket.close();

        return response.toByteArray();
    }*/

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        
        //Open connection with server
        Socket socket = new Socket(hostname, port);
        byte[] serverResponse = new byte[512];
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        
        //Contact server
        socket.getOutputStream().flush();
        socket.getOutputStream().write(toServerBytes);
        
        InputStream inputStream = socket.getInputStream();
        int length = inputStream.read(serverResponse);

        //Server responds
        while(length != -1) {
            //response.write(serverResponse);
            response.write(serverResponse, 0, length);
            length = inputStream.read(serverResponse);
        }
        
        socket.close();

        return response.toByteArray();
    }
}
