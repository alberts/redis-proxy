package redis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisClient {

    private static int PORT = 6379;
    private String hostname;

    public RedisClient(String hostname) {
        this.hostname = hostname;
    }

    public Object get(String key) {
        return null;
    }

}
