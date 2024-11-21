package com.example.demo.signaturecontainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignatureContainer;

/**
 * Implements {@link IExternalSignatureContainer}. Represents the blank signature container
 * to be signed afterward in the complete signature step.
 */
public class PreSignatureContainer implements IExternalSignatureContainer {
    private PdfDictionary sigDic;
    private byte hash[];
  
    public PreSignatureContainer(PdfName filter, PdfName subFilter) {
        sigDic = new PdfDictionary();
        sigDic.put(PdfName.Filter, filter);
        sigDic.put(PdfName.SubFilter, subFilter);
    }
  
    @Override
    public byte[] sign(InputStream data) throws GeneralSecurityException {
        String hashAlgorithm = "SHA256";
        BouncyCastleDigest digest = new BouncyCastleDigest();

        try {
            this.hash = DigestAlgorithms.digest(data, digest.getMessageDigest(hashAlgorithm));
        } catch (IOException e) {
            throw new GeneralSecurityException("PreSignatureContainer signing exception",e);
        }

        return new byte[0];
    }

    @Override
    public void modifySigningDictionary(PdfDictionary signDic) {
        signDic.putAll(sigDic);
    }

    public byte[] getHash() {
        return hash;
    }

    public void setHash(byte hash[]) {
        this.hash = hash;
    }
}