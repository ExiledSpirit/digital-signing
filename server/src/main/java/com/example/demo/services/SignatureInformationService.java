package com.example.demo.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.List;

import org.bouncycastle.asn1.tsp.TSTInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.annot.PdfWidgetAnnotation;
import com.itextpdf.signatures.CertificateInfo;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignaturePermissions;
import com.itextpdf.signatures.SignatureUtil;
import com.itextpdf.signatures.TimestampConstants;

@Service
public class SignatureInformationService {
    public SignatureInformationService() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public SignaturePermissions inspectSignature(PdfDocument pdfDoc, SignatureUtil signUtil, PdfAcroForm form,
            String name, SignaturePermissions perms) throws GeneralSecurityException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        List<PdfWidgetAnnotation> widgets = form.getField(name).getWidgets();

        // Check the visibility of the signature annotation
        if (widgets != null && widgets.size() > 0) {
            Rectangle pos = widgets.get(0).getRectangle().toRectangle();
            int pageNum = pdfDoc.getPageNumber(widgets.get(0).getPage());
            if (pos.getWidth() == 0 || pos.getHeight() == 0) {
                System.out.println("Invisible signature");
            } else {
                System.out.println(String.format("Field on page %s; llx: %s, lly: %s, urx: %s; ury: %s",
                        pageNum, pos.getLeft(), pos.getBottom(), pos.getRight(), pos.getTop()));
            }
        }

        /* Find out how the message digest of the PDF bytes was created,
         * how these bytes and additional attributes were signed
         * and how the signed bytes are stored in the PDF
         */
        PdfPKCS7 pkcs7 = verifySignature(signUtil, name);
        System.out.println("Digest algorithm: " + pkcs7.getDigestAlgorithm());
        System.out.println("Encryption algorithm: " + pkcs7.getHashAlgorithm());
        System.out.println("Filter subtype: " + pkcs7.getFilterSubtype());

        // Get the signing certificate to find out the name of the signer.
        X509Certificate cert = (X509Certificate) pkcs7.getSigningCertificate();
        System.out.println("Name of the signer: " + CertificateInfo.getSubjectFields(cert).getField("CN"));
        if (pkcs7.getSignName() != null) {
            System.out.println("Alternative name of the signer: " + pkcs7.getSignName());
        }

        // Get the signing time
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd");

        /* Mind that the getSignDate() method is not that secure as timestamp
         * because it's based only on signature author claim. I.e. this value can only be trusted
         * if signature is trusted and it cannot be used for signature verification.
         */
        System.out.println("Signed on: " + date_format.format(pkcs7.getSignDate().getTime()));

        /* If a timestamp was applied, retrieve information about it.
         * Timestamp is a secure source of signature creation time,
         * because it's based on Time Stamping Authority service.
         */
        if (TimestampConstants.UNDEFINED_TIMESTAMP_DATE != pkcs7.getTimeStampDate()) {
            System.out.println("TimeStamp: " + date_format.format(pkcs7.getTimeStampDate().getTime()));
            TSTInfo ts = ((TSTInfo) pkcs7.getTimeStampToken().getTimeStampInfo().toASN1Structure());
            System.out.println("TimeStamp service: " + ts.getTsa());
            System.out.println("Timestamp verified? " + pkcs7.verifyTimestampImprint());
        }

        System.out.println("Location: " + pkcs7.getLocation());
        System.out.println("Reason: " + pkcs7.getReason());

        /* If you want less common entries than PdfPKCS7 object has, such as the contact info,
         * you should use the signature dictionary and get the properties by name.
         */
        PdfDictionary sigDict = signUtil.getSignatureDictionary(name);
        PdfString contact = sigDict.getAsString(PdfName.ContactInfo);
        if (contact != null) {
            System.out.println("Contact info: " + contact);
        }

        /* Every new signature can add more restrictions to a document, but it can't take away previous restrictions.
         * So if you want to retrieve information about signatures restrictions, you need to pass
         * the SignaturePermissions instance of the previous signature, or null if there was none.
         */
        perms = new SignaturePermissions(sigDict, perms);
        System.out.println("Signature type: " + (perms.isCertification() ? "certification" : "approval"));
        System.out.println("Filling out fields allowed: " + perms.isFillInAllowed());
        System.out.println("Adding annotations allowed: " + perms.isAnnotationsAllowed());
        for (SignaturePermissions.FieldLock lock : perms.getFieldLocks()) {
            System.out.println("Lock: " + lock.toString());
        }

        return perms;
    }

    public PdfPKCS7 verifySignature(SignatureUtil signUtil, String name) throws GeneralSecurityException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PdfPKCS7 pkcs7 = signUtil.readSignatureData(name);

        Field digestAttrField = pkcs7.getClass().getDeclaredField("digestAttr");
        digestAttrField.setAccessible(true);
        byte[] digestAttr = (byte[]) digestAttrField.get(pkcs7);

        System.out.println(byteArrayToString(digestAttr));
        System.out.println("Signature covers whole document: " + signUtil.signatureCoversWholeDocument(name));
        System.out.println("Document revision: " + signUtil.getRevision(name) + " of " + signUtil.getTotalRevisions());
        System.out.println("Integrity check O? " + pkcs7.verifySignatureIntegrityAndAuthenticity());
        Certificate[] certChain = pkcs7.getSignCertificateChain();

        return pkcs7;
    }

    private String byteArrayToString(byte[] ba) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[ba.length * 2];
        for(int j = 0; j < ba.length; j++) {
            int v = ba[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[j >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void inspectSignatures(byte[] file) throws IOException, GeneralSecurityException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(file)));
        PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, false);
        SignaturePermissions perms = null;
        SignatureUtil signUtil = new SignatureUtil(pdfDoc);
        List<String> names = signUtil.getSignatureNames();

        for (String name : names) {
            System.out.println("===== " + name + " =====");
            perms = inspectSignature(pdfDoc, signUtil, form, name, perms);
        }
    }
}
