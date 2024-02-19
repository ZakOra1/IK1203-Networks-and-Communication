package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
    //  Shutdown controls if the client closes the connection
    //  Timeout decides maximum time (in ms) to wait for new data
    //  Limit controls maximum data (in bytes) to recive before returning

    private boolean shutdown;
    private Integer timeout, limit;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;

    }

    public TCPClient() {

    }

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        Socket socket = new Socket(hostname, port);
        ByteArrayOutputStream response = new ByteArrayOutputStream();

        // Set the socket timeout if specified
        if (timeout != null) {
            socket.setSoTimeout(timeout);
        }

        try {
            // Contact server
            socket.getOutputStream().write(toServerBytes);

            // Shutdown the outgoing connection if specified
            if (shutdown) {
                socket.shutdownOutput();
            }

            InputStream inputStream = socket.getInputStream();
            byte[] serverResponse = new byte[512];
            int length;
            int totalBytesRead = 0;

            while ((length = inputStream.read(serverResponse)) != -1) {
                response.write(serverResponse, 0, length);
                totalBytesRead += length;

                if (limit != null && totalBytesRead >= limit) {
                    break;
                }
            }
        } catch (SocketTimeoutException e) {
            // Handle socket timeout
            System.err.println("Socket timeout occurred.");
        }

        socket.close();

        return response.toByteArray();
    }
}
