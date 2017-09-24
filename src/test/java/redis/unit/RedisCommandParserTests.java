package redis.unit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import redis.RedisCommandParser;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisCommandParserTests {

    @Test
    public void testValidBulkString() {
        Assertions.assertThat(RedisCommandParser.parseBulkString("$6\r\nfoobar\r\n"))
                .contains("foobar");
        Assertions.assertThat(RedisCommandParser.parseBulkString("$0\r\n\r\n"))
                .contains("");
        Assertions.assertThat(RedisCommandParser.parseBulkString("$-1\r\n"))
                .isEqualTo(null);
    }

    @Test
    public void testInvalidBulkString() {
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseBulkString("$5\r\nfoobar\r\n"));
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseBulkString("$6\rfoobar\r\n"));
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseBulkString("$6\r\nfoobar\r"));
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseBulkString("$6\r\nfobar\r\n"));
    }

    @Test
    public void testValidGet() {
        Assertions.assertThat(RedisCommandParser.parseGet("*2\r\n$3\r\nGET\r\n$6\r\nfoobar\r\n"))
                .contains("foobar");
        Assertions.assertThat(RedisCommandParser.parseGet("*2\r\n$3\r\nGET\r\n$0\r\n\r\n"))
                .contains("");
        Assertions.assertThat(RedisCommandParser.parseGet("*2\r\n$3\r\nGET\r\n$-1\r\n"))
                .isEqualTo(null);
    }

    @Test
    public void testInvalidGet() {
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseGet("*2\r\n$3\r\nGET\r\n$5\r\nfoobar\r\n"));
        Assertions.assertThatThrownBy(() -> RedisCommandParser.parseGet("*2\r\n$3\r\nPUT\r\n$6\r\nfoobar\r\n"));
    }

}
