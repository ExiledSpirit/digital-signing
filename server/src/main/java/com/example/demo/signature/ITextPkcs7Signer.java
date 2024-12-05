package com.example.demo.signature;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;

@Component
public class ITextPkcs7Signer implements Pkcs7Signer {
    private static final String HASH_ALGORITHM = DigestAlgorithms.SHA256;

    @Override
    public byte[] sign(byte[] data, PrivateKey privateKey, Certificate[] certificateChain) throws GeneralSecurityException {
        PdfPKCS7 pkcs7 = new PdfPKCS7(null, certificateChain, HASH_ALGORITHM, null, new BouncyCastleDigest(), false);
        byte[] attributes = pkcs7.getAuthenticatedAttributeBytes(data, PdfSigner.CryptoStandard.CMS, null, null);
        PrivateKeySignature signature = new PrivateKeySignature(privateKey, HASH_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        byte[] attrSign = signature.sign(attributes);
        pkcs7.setExternalDigest(attrSign, null, signature.getEncryptionAlgorithm());
        return pkcs7.getEncodedPKCS7(data);
    }
}
