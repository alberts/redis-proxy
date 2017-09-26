package redis;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bchen on 9/24/17.
 */
public class RedisProxy {

    public static int REDIS_PORT = 6379;
    public static int PROXY_PORT = 7379;

    private RespClient redisClient;
    private LRULoadingCache cache;
    private ExecutorService executorService;

    public RedisProxy(String redisHost, int capacity, long cacheExpiryInMs) {
        redisClient = new RespClient(redisHost, REDIS_PORT);
        cache = new LRULoadingCache(capacity, cacheExpiryInMs, (command) -> redisClient.sendRequest(command));
        executorService = Executors.newFixedThreadPool(5);
    }

    public void handleRequest(Socket connectionSocket) {
        executorService.submit(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                DataOutputStream out = new DataOutputStream(connectionSocket.getOutputStream());

                String command = RedisParser.readRespMessage(in);;
                String key = RedisParser.parseGet(command);
                String response = cache.get(command, key);
                out.writeBytes(response);

                in.close();
                out.close();
                connectionSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Required arguments: [redisHost] [capacity] [cacheExpiryInMs]");
            return;
        }
        String redisHost = args[0];
        int capacity = Integer.parseInt(args[1]);
        long cacheExpiryInMs = Long.parseLong(args[2]);
        RedisProxy proxy = new RedisProxy(redisHost, capacity, cacheExpiryInMs);
        try {
            ServerSocket socket = new ServerSocket(PROXY_PORT);
            System.out.println("Ready to accept connections");
            while (true) {
                Socket connectionSocket = socket.accept();
                System.out.println("Connection from " + connectionSocket.getInetAddress() + ":" + connectionSocket.getPort() + " accepted");
                proxy.handleRequest(connectionSocket);
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
    }

}
