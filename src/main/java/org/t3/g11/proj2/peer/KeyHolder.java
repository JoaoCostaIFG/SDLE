package org.t3.g11.proj2.peer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyHolder {
    private final KeyFactory keyFactory;
    private final Cipher cipher;
    private final String keyInstance;
    private final int keySize;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    public KeyHolder(String keyInstance, int keySize) throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.keyInstance = keyInstance;
        this.keySize = keySize;
        this.keyFactory = KeyFactory.getInstance(keyInstance);
        this.cipher = Cipher.getInstance(keyInstance);
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(this.keyInstance);
        keyPairGenerator.initialize(this.keySize, secureRandom);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        this.setPrivateKey(keyPair.getPrivate());
        this.setPublicKey(keyPair.getPublic());
        return keyPair;
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void clear() {
        this.privateKey = null;
        this.publicKey = null;
    }

    public byte[] encrypt(byte[] buf, PrivateKey privateKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        this.cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        return this.cipher.doFinal(buf);
    }

    public byte[] encrypt(byte[] buf) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return this.encrypt(buf, this.privateKey);
    }

    public byte[] decrypt(byte[] buf, PublicKey publicKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        this.cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return this.cipher.doFinal(buf);
    }

    public byte[] decrypt(byte[] buf) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return this.decrypt(buf, this.publicKey);
    }

    public PublicKey genPubKey(byte[] keyBuf) throws InvalidKeySpecException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBuf);
        return keyFactory.generatePublic(pubKeySpec);
    }

    public PublicKey genPubKey(String keyStr) throws InvalidKeySpecException {
        return this.genPubKey(Base64.getDecoder().decode(keyStr));
    }

    public static String encodeKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static void writeKeyToFile(Key key, String username) throws IOException {
        byte[] encoded = key.getEncoded();
        FileOutputStream keyfos = new FileOutputStream(username + ".priv");
        keyfos.write(encoded);
        keyfos.close();
    }
}
