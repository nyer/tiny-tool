import java.util.Scanner;

/**
 * variable byte code
 */

public class VariableByteCode {
    public static final long HIGHT_ONE = 1 << 63;

    public static final byte END_BYTE_MARK = (byte)(1 << 7);
    public static final int LAST_SEVEN_BIT = (1 << 7) - 1;

    public static final long TWO_BYTE_LIMIT = 1L << 7;
    public static final long THREE_BYTE_LIMIT = 1L << 14;
    public static final long FOUR_BYTE_LIMIT = 1L << 21;
    public static final long FIVE_BYTE_LIMIT = 1L << 28;
    public static final long SIX_BYTE_LIMIT = 1L << 35;
    public static final long SEVEN_BYTE_LIMIT = 1L << 42;
    public static final long EIGHT_BYTE_LIMIT = 1L << 49;
    public static final long NINE_BYTE_LIMIT = 1L << 56;
    public static final long TEN_BYTE_LIMIT = 1L << 63;

    public static byte[] encode(long v) {
        // ensure the byte array size
        int size = 1;
        if (v < 0) {
            size = 10;
        } else if (v >= NINE_BYTE_LIMIT) {
            size = 9;
        } else if (v >= EIGHT_BYTE_LIMIT) {
            size = 8;
        } else if (v >= SEVEN_BYTE_LIMIT) {
            size = 7;
        } else if (v >= SIX_BYTE_LIMIT) {
            size = 6;
        } else if (v >= FIVE_BYTE_LIMIT) {
            size = 5;
        } else if (v >= FOUR_BYTE_LIMIT) {
            size = 4;
        } else if (v >= THREE_BYTE_LIMIT) {
            size = 3;
        } else if (v >= TWO_BYTE_LIMIT) {
            size = 2;
        } else {
            size = 1;
        }

        byte[] bytes = new byte[size];

        int i = size - 1;
        do {
            bytes[i] = (byte)(v & LAST_SEVEN_BIT);
            v = v >>> 7;
            i --;
        } while (v >= 128 ); 
        
        if (v > 0)
            bytes[i] = (byte)v;
        bytes[size -1] = (byte)(bytes[size -1] | END_BYTE_MARK);

        return bytes;
    }

    public static long decode(byte[] b) {
        long v = 0;
        for (int i = 0, s = b.length;i < s; i ++){
            v = v << 7;
            b[i] = (byte)(b[i] & LAST_SEVEN_BIT);
            v = v | b[i];
        }

        return v;
    }

    public static void printByte(byte b) {
        int pos = 0;
        while (pos < 8) {
            if ((b & HIGHT_ONE) == 0)
                System.out.print(0);
            else
                System.out.print(1);
            b = (byte)(b << 1);
            pos ++;
        }

        System.out.println();
    }

    public static void main(String[] args) {
        Scanner scaner = new Scanner(System.in);
        while (true) {
            long v = scaner.nextLong();
            byte[] bytes = encode(v);
            for (int i = 0, s = bytes.length;i < s;i ++)
                printByte(bytes[i]);
            System.out.println(decode(bytes));
        }
    }
}

