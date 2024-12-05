package com.example.demo.signature.containers;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import com.example.demo.helpers.SignTestPortHelper;
import com.example.demo.signature.SignatureConstraints;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignatureContainer;

public class DigestCalcBlankSignatureContainer implements IExternalSignatureContainer {
    private final PdfName filter;
    private final PdfName subFilter;

    private byte[] docBytesHash;

    public DigestCalcBlankSignatureContainer(PdfName filter, PdfName subFilter) {
        this.filter = filter;
        this.subFilter = subFilter;
    }

    public byte[] getDocBytesHash() {
        return docBytesHash;
    }

    public byte[] sign(InputStream docBytes) throws GeneralSecurityException {
        try {
            docBytesHash = calcDocBytesHash(docBytes);
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
        return new byte[0];
    }

    public void modifySigningDictionary(PdfDictionary signDic) {
        signDic.put(PdfName.Filter, filter);
        signDic.put(PdfName.SubFilter, subFilter);
    }
  
    private byte[] calcDocBytesHash(InputStream docBytes) throws IOException, GeneralSecurityException {
        byte[] docBytesHash = null;
        docBytesHash = DigestAlgorithms.digest(docBytes, SignTestPortHelper.getMessageDigest(SignatureConstraints.HASH_ALGORITHM));

        return docBytesHash;
    }
}
