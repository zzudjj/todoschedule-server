package com.djj.todoscheduleserver.utils;

import lombok.extern.slf4j.Slf4j;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 微信工具类
 * 提供微信相关的工具方法
 */
@Slf4j
public class WechatUtils {

    /**
     * 验证微信服务器签名
     * 
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param token 令牌
     * @return 验证是否通过
     */
    public static boolean checkSignature(String signature, String timestamp, String nonce, String token) {
        // 按字典顺序排序三个字符串
        String[] arr = new String[]{token, timestamp, nonce};
        Arrays.sort(arr);

        // 将三个字符串连接成一个字符串
        StringBuilder content = new StringBuilder();
        for (String s : arr) {
            content.append(s);
        }
        
        // SHA-1加密
        String tmpStr = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(content.toString().getBytes());
            tmpStr = byteToStr(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("未找到SHA-1算法", e);
            return false;
        }

        // 与签名进行比较
        return tmpStr != null && tmpStr.equals(signature.toUpperCase());
    }
    
    /**
     * 将字节转换为十六进制字符
     */
    public static String byteToHexStr(byte mByte) {
        char[] Digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
                'B', 'C', 'D', 'E', 'F'};
        char[] tempArr = new char[2];
        tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
        tempArr[1] = Digit[mByte & 0X0F];
        return new String(tempArr);
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    public static String byteToStr(byte[] byteArray) {
        StringBuilder strDigest = new StringBuilder();
        for (byte b : byteArray) {
            strDigest.append(byteToHexStr(b));
        }
        return strDigest.toString();
    }
} 