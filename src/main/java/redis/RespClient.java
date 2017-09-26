package redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A simple, thread-safe client for RESP over TCP.
 */
public final class RespClient {

    private String hostname;
    private int port;

    public RespClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public String sendRequest(String command) {
        try {
            Socket socket = new Socket(hostname, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.print(command);
            out.flush();

            String response = RedisParser.readRespMessage(in);

            out.close();
            in.close();
            socket.close();
            return response;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
