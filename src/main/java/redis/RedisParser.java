package redis;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by bchen on 9/23/17.
 */
public final class RedisParser {

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

    public static String readRespMessage(BufferedReader reader) {
        String inputUntilFirstLineBreak = readUntilLineBreak(reader);
        StringBuilder sb = new StringBuilder();
        sb.append(inputUntilFirstLineBreak);
        char c = inputUntilFirstLineBreak.charAt(0);

        if (c == '+' || c == '-' || c == ':') {
            // simple string, error, or integer -- no more parts
        } else if (c == '$') { // bulk string
            if (inputUntilFirstLineBreak.charAt(1) == '-' && inputUntilFirstLineBreak.charAt(2) == '1') { // null -- no more parts
                return inputUntilFirstLineBreak;
            }
            int length = getLengthFromString(inputUntilFirstLineBreak, 1);
            String s = readFixedLength(reader, length + 2);
            if (!s.endsWith("\r\n")) {
                throw new RuntimeException("Unexpected input: " + s);
            }
            sb.append(s);
        } else if (c == '*') { // array
            int length = getLengthFromString(inputUntilFirstLineBreak, 1);
            for (int i = 0; i < length; i++) {
                sb.append(readRespMessage(reader));
            }
        }

        return sb.toString();
    }

    private static int getLengthFromString(String str, int offset) {
        int length = 0;
        while (str.charAt(offset) != '\r') {
            length = length * 10 + Integer.parseInt(str.charAt(offset) + "");
            offset++;
        }
        return length;
    }

    private static String readUntilLineBreak(BufferedReader reader) {
        try {
            StringBuilder sb = new StringBuilder();
            char previous = '0';
            char current = '0';
            int c;
            while (previous != '\r' || current != '\n') {
                previous = current;
                c = reader.read();
                if (c == -1) {
                    break;
                } else {
                    current = (char) c;
                }
                sb.append(current);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFixedLength(BufferedReader reader, int length) {
        try {
            StringBuilder sb = new StringBuilder();
            char[] cbuf = new char[1024];
            int lengthLeft = length;
            int k;
            while (lengthLeft > 0) {
                k = reader.read(cbuf, 0, length > 1024 ? 1024 : length);
                if (k > 0) {
                    if (k > length) {
                        throw new RuntimeException("WTF");
                    }
                    lengthLeft -= k;
                    sb.append(cbuf, 0, k);
                } else if (lengthLeft > 0) {
                    throw new RuntimeException("Stream closed unexpectedly");
                } else {
                    break;
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
