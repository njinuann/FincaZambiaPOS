package APX;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;
import com.sun.crypto.provider.SunJCE;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class BRCrypt
{
    private static Cipher encipher = null;
    private static Cipher decipher = null;
    private static final java.security.spec.KeySpec keySpec = new PBEKeySpec(String.valueOf("This is supernova project !").toCharArray());

    private static final byte salt[] =
    {
        -87, -101, -56, 50, 86, 53, -29, 3
    };
    private static final java.security.spec.AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, 19);
    private static SecretKey key = null;

    static
    {
        try
        {
            Security.addProvider(new SunJCE());
            key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public BRCrypt()
    {
        initialize();
    }

    public static void initialize()
    {
        try
        {
            encipher = Cipher.getInstance(key.getAlgorithm());
            decipher = Cipher.getInstance(key.getAlgorithm());
            reset();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            reset();
        }
    }

    public static void reset()
    {
        try
        {
            encipher.init(1, key, paramSpec);
            decipher.init(2, key, paramSpec);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static String encrypt(String plainStr)
    {
        try
        {
            if (plainStr != null)
            {
                if (encipher == null)
                {
                    initialize();
                }
                return (new BASE64Encoder()).encode(encipher.doFinal(plainStr.getBytes(StandardCharsets.UTF_8)));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            reset();
        }
        return plainStr;
    }

    public static String decrypt(String encryptedStr)
    {
        try
        {
            if (encryptedStr != null)
            {
                if (decipher == null)
                {
                    initialize();
                }
                return new String(decipher.doFinal((new BASE64Decoder()).decodeBuffer(encryptedStr)), StandardCharsets.UTF_8);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            reset();
        }
        return encryptedStr;
    }

    public static boolean isEncrypted(String text)
    {
        try
        {
            if (text != null)
            {
                if (text.length() >= 3)
                {
                    if (decipher == null)
                    {
                        initialize();
                    }
                    return !text.equals(new String(decipher.doFinal((new BASE64Decoder()).decodeBuffer(text)), StandardCharsets.UTF_8));
                }
            }
        }
        catch (Exception ex)
        {
            reset();
        }
        return false;
    }   
    public static void main(String[] args)
    {
        System.err.println(encrypt("BRN3P23cUr1T7"));
         System.err.println(""+decrypt("ACdFgyoXow8uEp5NQ6xqXQ=="));
    }
}
