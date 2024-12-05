package com.example.demo.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.springframework.core.io.ClassPathResource;

public final class Pkcs12FileHelper {
    private Pkcs12FileHelper() {}

    public static Certificate[] readFirstChain(String p12FileName, char[] ksPass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return getKeyStoreDetails(p12FileName, ksPass, null, true);
    }

    public static PrivateKey readFirstKey(String p12FileName, char[] ksPass, char[] keyPass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return getKeyStoreDetails(p12FileName, ksPass, keyPass, false);
    }


    private static <T> T getKeyStoreDetails(String p12FileName, char[] ksPass, char[] keyPass, boolean isChain) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        T result = null;

        try (InputStream keyStoreStream = new ClassPathResource(p12FileName).getInputStream()) {
            KeyStore p12 = KeyStore.getInstance("pkcs12");
            p12.load(keyStoreStream, ksPass);

            Enumeration<String> aliases = p12.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (p12.isKeyEntry(alias)) {
                    if(isChain) {
                        result = (T) p12.getCertificateChain(alias);
                    } else {
                        result = (T) p12.getKey(alias, keyPass);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new FileNotFoundException("Could not load keystore file from classpath: " + p12FileName + ".  " + e.getMessage());
        }
        return result;
    }

    public static KeyStore initStore(String p12FileName, char[] ksPass) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        try (InputStream keyStoreStream = new ClassPathResource(p12FileName).getInputStream()) {
            KeyStore p12 = KeyStore.getInstance("PKCS12", "BC");
            p12.load(keyStoreStream, ksPass);
            return p12;
        } catch (IOException e) {
            throw new FileNotFoundException("Could not load keystore file from classpath: " + p12FileName + ". " + e.getMessage());
        }
    }
}
