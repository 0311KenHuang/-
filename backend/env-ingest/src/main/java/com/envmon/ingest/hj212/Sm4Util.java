package com.envmon.ingest.hj212;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * SM4 工具（BouncyCastle）。
 *
 * JDK 8 没有内置国密算法，必须依赖 BC。
 * HJ 212—2025 使用 SM4-128-ECB，填充方式按现场设备实际实现为准，
 * 这里同时提供 NoPadding 与 PKCS5 两种，交由调用方选择。
 */
public final class Sm4Util {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Sm4Util() {
    }

    public static byte[] decryptEcbNoPadding(byte[] key, byte[] data) throws Exception {
        return doFinal(Cipher.DECRYPT_MODE, key, "SM4/ECB/NoPadding", data);
    }

    public static byte[] decryptEcbPkcs5(byte[] key, byte[] data) throws Exception {
        return doFinal(Cipher.DECRYPT_MODE, key, "SM4/ECB/PKCS5Padding", data);
    }

    public static byte[] encryptEcbPkcs5(byte[] key, byte[] data) throws Exception {
        return doFinal(Cipher.ENCRYPT_MODE, key, "SM4/ECB/PKCS5Padding", data);
    }

    public static String decryptHex(String keyHex, String dataHex) throws Exception {
        byte[] plain = decryptEcbPt(keyHex, dataHex);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** 设备侧常用十六进制密钥/密文，长度不足 16 字节会被视为非法 */
    public static byte[] decryptEcbPt(String keyHex, String dataHex) throws Exception {
        byte[] key = hexToBytes(keyHex);
        if (key.length != 16) {
            throw new IllegalArgumentException("SM4 密钥必须为 16 字节");
        }
        return decryptEcbNoPadding(key, hexToBytes(dataHex));
    }

    private static byte[] doFinal(int mode, byte[] key, String transformation, byte[] data)
            throws Exception {
        SecretKeySpec spec = new SecretKeySpec(key, "SM4");
        Cipher cipher = Cipher.getInstance(transformation, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(mode, spec);
        return cipher.doFinal(data);
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
