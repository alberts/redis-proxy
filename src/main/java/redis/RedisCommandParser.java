package redis;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisCommandParser {

    private static String GET_PREFIX = "*2\r\n$3\r\nGET\r\n";

    public static String parseGet(String getCommand) {
        if (!getCommand.startsWith(GET_PREFIX)) {
            throw new RuntimeException("Invalid input: " + getCommand);
        }
        return parseBulkString(getCommand.substring(GET_PREFIX.length()));
    }

    public static String parseBulkString(String bulkString) {
        if (bulkString.equals("$-1\r\n")) {  // Null Bulk String
            return null;
        }
        if (!bulkString.startsWith("$") || !bulkString.endsWith("\r\n")) {
            throw new RuntimeException("Invalid input: " + bulkString);
        }
        int indexOfFirstNewline = bulkString.indexOf("\r\n");
        String stringAfterFirstNewline = bulkString.substring(indexOfFirstNewline + 2);
        int length = Integer.parseInt(bulkString.substring(1, indexOfFirstNewline));
        if (stringAfterFirstNewline.length() != length + 2) {
            throw new RuntimeException("Invalid input: " + bulkString);
        }
        return stringAfterFirstNewline.substring(0, length);
    }

}
