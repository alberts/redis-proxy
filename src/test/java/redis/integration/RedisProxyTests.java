package redis.integration;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import redis.RedisProxy;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisProxyTests {

    private RedisProxy redisProxy;

    @Before
    public void before() {
        redisProxy = new RedisProxy("localhost");
    }

    @Test
    public void testProxiedGet() {
        String set = "*3\r\n$3\r\nSET\r\n$4\r\nbeck\r\n$4\r\nchen\r\n";
        String get = "*2\r\n$3\r\nGET\r\n$4\r\nbeck\r\n";
        redisProxy.execute(set);
        Assertions.assertThat(redisProxy.execute(get))
                .isEqualTo("$4\r\nchen\r\n");
    }

}
