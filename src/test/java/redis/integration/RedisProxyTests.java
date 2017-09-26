package redis.integration;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.RespClient;
import redis.RedisProxy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bchen on 9/24/17.
 */
public class RedisProxyTests {

    private static RespClient redisClient;
    private static RespClient proxyClient;

    private static String getName = "*2\r\n$3\r\nGET\r\n$4\r\nname\r\n";
    private static String getLanguage = "*2\r\n$3\r\nGET\r\n$8\r\nlanguage\r\n";
    private static String getHometown = "*2\r\n$3\r\nGET\r\n$8\r\nhometown\r\n";
    private static String getSex = "*2\r\n$3\r\nGET\r\n$3\r\nsex\r\n";

    @BeforeClass
    public static void beforeClass() {
        redisClient = new RespClient("localhost", RedisProxy.REDIS_PORT);
        proxyClient = new RespClient("localhost", RedisProxy.PROXY_PORT);

        redisClient.sendRequest("*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$4\r\nbeck\r\n"); // name -> beck
        redisClient.sendRequest("*3\r\n$3\r\nSET\r\n$8\r\nlanguage\r\n$4\r\njava\r\n"); // language -> java
        redisClient.sendRequest("*3\r\n$3\r\nSET\r\n$8\r\nhometown\r\n$5\r\nchina\r\n"); // hometown -> china
        redisClient.sendRequest("*3\r\n$3\r\nSET\r\n$3\r\nsex\r\n$4\r\nmale\r\n"); // sex -> male
    }

    @Test
    public void testProxiedGet() {
        Assertions.assertThat(proxyClient.sendRequest(getName))
                .isEqualTo("$4\r\nbeck\r\n");
    }

    @Test
    public void testParallelRequests() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        AtomicBoolean correct = new AtomicBoolean(true);
        List<Future> futures = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            futures.add(executorService.submit(() -> Assertions.assertThat(proxyClient.sendRequest(getName)).isEqualTo("$4\r\nbeck\r\n")));
            futures.add(executorService.submit(() -> Assertions.assertThat(proxyClient.sendRequest(getLanguage)).isEqualTo("$4\r\njava\r\n")));
            futures.add(executorService.submit(() -> Assertions.assertThat(proxyClient.sendRequest(getHometown)).isEqualTo("$5\r\nchina\r\n")));
            futures.add(executorService.submit(() -> Assertions.assertThat(proxyClient.sendRequest(getSex)).isEqualTo("$4\r\nmale\r\n")));
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        for (Future<String> future : futures) {
            future.get();
        }
        Assertions.assertThat(correct.get()).isEqualTo(true);
    }

}
