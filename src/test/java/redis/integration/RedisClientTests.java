package redis.integration;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.RedisProxy;
import redis.RespClient;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisClientTests {

    private static RespClient redisClient;

    @BeforeClass
    public static void beforeClass() {
        redisClient = new RespClient("localhost", RedisProxy.REDIS_PORT);
    }

    @Test
    public void testGet() {
        String set = "*3\r\n$3\r\nSET\r\n$4\r\nbeck\r\n$4\r\nchen\r\n";
        String get = "*2\r\n$3\r\nGET\r\n$4\r\nbeck\r\n";
        redisClient.sendRequest(set);
        Assertions.assertThat(redisClient.sendRequest(get))
                .isEqualTo("$4\r\nchen\r\n");
    }

}
