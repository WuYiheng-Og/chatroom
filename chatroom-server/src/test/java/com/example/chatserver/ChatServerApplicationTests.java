package com.example.chatserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@SpringBootTest
public class ChatServerApplicationTests {

    @Test
    public void testBytArr() throws UnsupportedEncodingException {
//        一个100长度的数组，取前20位转化为字符串

        String str1 = "abc123中国";

        byte[] gbks = str1.getBytes("gbk");
        System.out.println(Arrays.toString(gbks));
        String str4 = new String(gbks,"gbk");
        System.out.println(str4);//没有乱码，原因：编码集和解码集一样
        byte[] idBytes = new byte[10];
        ByteBuffer byteBuffer = ByteBuffer.allocate(20);
        byteBuffer.put(idBytes);
        byteBuffer.put(gbks);

        byte[] bytesGet = byteBuffer.array();
        String user = new String(bytesGet,"gbk");
        String[] split = user.split("\0+");
        System.out.println(split[1]);
    }

}
