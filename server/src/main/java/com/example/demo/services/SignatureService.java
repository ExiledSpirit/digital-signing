
package com.example.demo.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import com.example.demo.dto.response.StartSigningResponse;
import com.example.demo.factory.SignatureAppearanceBuilder;
import com.example.demo.helpers.Pkcs12FileHelper;
import com.example.demo.session.SigningSession;
import com.example.demo.signature.Pkcs7Signer;
import com.example.demo.signature.SignatureConstraints;
import com.example.demo.signature.containers.DigestCalcBlankSignatureContainer;
import com.example.demo.signature.containers.ReadySignatureContainer;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.IExternalSignatureContainer;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;

@Service
public class SignatureService {
    private final Pkcs7Signer pkcs7Signer;

    private final SigningSession signingSession;

    public SignatureService(Pkcs7Signer pkcs7Signer, SigningSession signingSession) {
        this.pkcs7Signer = pkcs7Signer;
        this.signingSession = signingSession;

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public byte[] locallySign(byte[] file, char[] password) throws IOException, GeneralSecurityException {
        ByteArrayInputStream input = new ByteArrayInputStream(file);
        ByteArrayOutputStream signedStream = new ByteArrayOutputStream();

        PdfReader reader = new PdfReader(input);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfSigner signer = new PdfSigner(reader, baos, new StampingProperties());
        signer.setCertificationLevel(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED);
        this.createSignatureAppearance(signer);

        // generates empty container and get bytes
        DigestCalcBlankSignatureContainer external = this.createBlankSignatureContainer();
        signer.signExternalContainer(external, SignatureConstraints.EXTERNAL_CONTAINER_ESTIMATED_SIZE);
        byte[] blankContainerBytes = external.getDocBytesHash();
        byte[] preSignedDocumentWithBlankContainerBytes = baos.toByteArray();

        // sign the hash
        String signCertFileName = "/certificates/new.p12";
        Certificate[] signChain = Pkcs12FileHelper.readFirstChain(signCertFileName, password);
        PrivateKey signPrivateKey = Pkcs12FileHelper.readFirstKey(signCertFileName, password, password);
        byte[] cmsSignature = this.pkcs7Signer.sign(blankContainerBytes, signPrivateKey, signChain);

        // fill the signature to the presigned document
        ReadySignatureContainer extSigContainer = new ReadySignatureContainer(cmsSignature);

        PdfDocument docToSign = new PdfDocument(new PdfReader(new ByteArrayInputStream(preSignedDocumentWithBlankContainerBytes)));
        PdfSigner.signDeferred(docToSign, signer.getFieldName(), signedStream, extSigContainer);
        docToSign.close();

        return signedStream.toByteArray();
    }

    public StartSigningResponse startRemoteSigning(byte[] file, String certContent) throws IOException, GeneralSecurityException, CertificateException {
        X509Certificate certificate = this.decodeCertificate(certContent);
        X509Certificate[] certificateChain = new X509Certificate[]{certificate};

        ByteArrayInputStream input = new ByteArrayInputStream(file);
        ByteArrayOutputStream preparedPdfStream = new ByteArrayOutputStream();
        
        PdfReader reader = new PdfReader(input);
        PdfSigner signer = new PdfSigner(reader, preparedPdfStream, new StampingProperties().useAppendMode());
        
        this.createSignatureAppearance(signer);
        signer.setCertificationLevel(PdfSigner.NOT_CERTIFIED);
        String fieldName = signer.getFieldName();
        
        // Calculate the hash using DigestCalcBlankSigner
        DigestCalcBlankSignatureContainer external = new DigestCalcBlankSignatureContainer(PdfName.Adobe_PPKLite, PdfName.Adbe_pkcs7_detached);
        signer.signExternalContainer(external, 8192); // Estimated size
        
        byte[] preSignedBytes = preparedPdfStream.toByteArray();
        byte[] beforeAttrBytes = external.getDocBytesHash();
        byte[] toSignHash = this.generateBytesToSign(beforeAttrBytes, certificateChain); // Get the hash
        
        // Store necessary data in the session
        this.signingSession.setPreparedPdfBytes(preSignedBytes);
        this.signingSession.setFieldName(fieldName);
        this.signingSession.setToSign(toSignHash);
        this.signingSession.setCertContent(certContent);
        this.signingSession.setBeforeAttr(beforeAttrBytes);
        
        // Return the prepared PDF and the hash to the client
        StartSigningResponse startSigningResponse = new StartSigningResponse();
        startSigningResponse.setPreparedPdfBytes(Base64.getEncoder().encodeToString(preSignedBytes));
        startSigningResponse.setToSignHash(Base64.getEncoder().encodeToString(toSignHash));

        return startSigningResponse;
    }

    public byte[] completeRemoteSigning(byte[] signedHash, byte[] preparedPdfBytes, String fieldName, byte[] beforeAttrBytes, String certContent) throws IOException, GeneralSecurityException, CertificateException {
        X509Certificate certificate = this.decodeCertificate(certContent);

        PdfPKCS7 pkcs7 = new PdfPKCS7(null, new X509Certificate[]{certificate}, SignatureConstraints.HASH_ALGORITHM, null, new BouncyCastleDigest(), false);

        pkcs7.setExternalDigest(signedHash, null, SignatureConstraints.DIGEST_ALGORITHM);
        byte[] signatureContent = pkcs7.getEncodedPKCS7(beforeAttrBytes);

        try (ByteArrayOutputStream signedPdfStream = new ByteArrayOutputStream();
            PdfDocument docToSign = new PdfDocument(new PdfReader(new ByteArrayInputStream(preparedPdfBytes)))) {

            IExternalSignatureContainer externalSignatureContainer = new ReadySignatureContainer(signatureContent);
            PdfSigner.signDeferred(docToSign, fieldName, signedPdfStream, externalSignatureContainer);
            docToSign.close();

            return signedPdfStream.toByteArray();
        }
    }

    private PdfSignatureAppearance createSignatureAppearance(PdfSigner signer) {
        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        SignatureAppearanceBuilder signatureAppearanceBuilder = new SignatureAppearanceBuilder(signer.getDocument(), appearance, true)
                .withLayer2Text("Assinatura");

        signer.setFieldName(signatureAppearanceBuilder.calculateFieldName());

        PdfSignatureAppearance newSignatureAppearance = signatureAppearanceBuilder.build();
        appearance.setPageRect(newSignatureAppearance.getPageRect());
        appearance.setLayer2Text(newSignatureAppearance.getLayer2Text());

        return newSignatureAppearance;
    }
    
    private byte[] generateBytesToSign(byte[] docBytes, Certificate[] chain) {
        byte[] attributes = null;
        try {
            PdfPKCS7 pkcs7 = new PdfPKCS7(null, chain, SignatureConstraints.HASH_ALGORITHM, null, new BouncyCastleDigest(), false);

            attributes = pkcs7.getAuthenticatedAttributeBytes(docBytes, PdfSigner.CryptoStandard.CMS, null, null);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        return attributes;
    }

    /**
     * Logic for converting base64 certificate into X509Certificate
     * 
     * @param base64Certificate Certificate encoded in base64.
     * @return {@link X509Certificate} instance of the converted certificate.
     * @throws CertificateException
     */
    private X509Certificate decodeCertificate(String base64Certificate) throws CertificateException {
        // Decode and load the certificate bytes
        byte[] certBytes = Base64.getDecoder().decode(base64Certificate);

        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certBytes));

        return certificate;
    }

    private DigestCalcBlankSignatureContainer createBlankSignatureContainer() {
        PdfName filter = PdfName.Adobe_PPKLite;
        PdfName subFilter = PdfName.Adbe_pkcs7_detached;
        return new DigestCalcBlankSignatureContainer(filter, subFilter);
    }
}

