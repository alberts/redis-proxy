package redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisProxy {

    private static int PORT = 6379;
    private String hostname;

    public RedisProxy(String hostname) {
        this.hostname = hostname;
    }

    public String execute(String command) {
        try {
            Socket socket = new Socket(hostname, PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.print(command);
            out.flush();

            StringBuilder sb = new StringBuilder();
            char[] cbuf = new char[1024];
            int k;
            while ((k = in.read(cbuf, 0, 1024)) != -1) {
                sb.append(cbuf, 0, k);
                if (k < 1024) {
                    break;
                }
            }

            out.close();
            in.close();
            socket.close();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
