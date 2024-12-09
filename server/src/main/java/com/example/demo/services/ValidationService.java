package com.example.demo.services;

import com.example.demo.dto.response.ValidationResult;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.*;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;
@Service
public class ValidationService {

  public ValidationResult validateSignature(byte[] signedContent) {
      ValidationResult result = new ValidationResult();

      try {
          // Initialize Bouncy Castle provider if not already done
          Security.addProvider(new BouncyCastleProvider());

          // Parse the CMS/PKCS#7 signed data
          CMSSignedData cmsSignedData = new CMSSignedData(signedContent);
          Store<X509CertificateHolder> certificates = cmsSignedData.getCertificates();
          SignerInformation signer = cmsSignedData.getSignerInfos().getSigners().iterator().next();

          // 1. Validate content type and message digest
          validateContentTypeAndDigest(signer, result);

          // 2. Get and validate signing certificate
          X509CertificateHolder signerCert = getCertificate(certificates, signer.getSID());
          if (signerCert == null) {
              result.addError("Signer certificate not found");
              return result;
          }

          // 3. Validate signature value
          validateSignatureValue(signer, signerCert, result);

          // 4. Validate certificate chain
          validateCertificateChain(signerCert, certificates, result);

          // 5. Validate signed attributes
          validateSignedAttributes(signer, result);

          // 6. Validate unsigned attributes
          validateUnsignedAttributes(signer, result);

      } catch (Exception e) {
        e.printStackTrace();
          result.addError("Error validating signature: " + e.getMessage());
      }

      return result;
  }

  private void validateContentTypeAndDigest(SignerInformation signer, ValidationResult result) {
      try {
          // Get the content type from signed attributes
          Attribute contentTypeAttr = signer.getSignedAttributes().get(CMSAttributes.contentType);
          if (contentTypeAttr == null) {
              result.addError("Missing content type attribute");
              return;
          }

          // Get the message digest from signed attributes
          Attribute messageDigestAttr = signer.getSignedAttributes().get(CMSAttributes.messageDigest);
          if (messageDigestAttr == null) {
              result.addError("Missing message digest attribute");
              return;
          }

          // Verify the message digest
          byte[] calculatedDigest = signer.getContentDigest();
          byte[] storedDigest = ((ASN1OctetString) messageDigestAttr.getAttrValues().getObjectAt(0))
                  .getOctets();

          if (Arrays.equals(calculatedDigest, storedDigest)) {
              result.addSuccess("The content-type and message digest present in the signed attributes match");
          } else {
              result.addError("Message digest mismatch");
          }

      } catch (Exception e) {
          result.addError("Error validating content type and digest: " + e.getMessage());
      }
  }

  private void validateSignatureValue(SignerInformation signer, 
                                    X509CertificateHolder signerCert, 
                                    ValidationResult result) {
      try {
          SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder()
              .setProvider(new BouncyCastleProvider())
              .build(signerCert);

          if (signer.verify(verifier)) {
              result.addSuccess("The signature value is correct");
          } else {
              result.addError("Invalid signature value");
          }
      } catch (Exception e) {
          result.addError("Error validating signature value: " + e.getMessage());
      }
  }

  private void validateCertificateChain(X509CertificateHolder signerCert, 
                                      Store<X509CertificateHolder> certificates, 
                                      ValidationResult result) {
      try {
          // Convert Bouncy Castle certificates to Java certificates
          List<X509Certificate> certChain = new ArrayList<>();
          JcaX509CertificateConverter converter = new JcaX509CertificateConverter()
              .setProvider(new BouncyCastleProvider());

          // Add signer certificate
          certChain.add(converter.getCertificate(signerCert));

          Collection<X509CertificateHolder> certHolders = certificates.getMatches(null);
          for (X509CertificateHolder certHolder : certHolders) {
              if (!certHolder.equals(signerCert)) {
                  certChain.add(converter.getCertificate(certHolder));
              }
          }

          // Load Brazilian root certificate
          X509Certificate rootCert = loadBrazilianRootCertificate();

          // Build certification path
          CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
          X509CertSelector selector = new X509CertSelector();
          selector.setCertificate(certChain.get(0));  // Signer certificate

          Set<TrustAnchor> trustAnchors = Collections.singleton(new TrustAnchor(rootCert, null));
          PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, selector);

          // Add intermediate certificates to the path
          CertStore intermediateCertStore = CertStore.getInstance("Collection",
              new CollectionCertStoreParameters(certChain),
              "BC");
          params.addCertStore(intermediateCertStore);

          // Disable CRL checking for now (implement proper CRL/OCSP checking in production)
          params.setRevocationEnabled(false);

          // Build and validate the certification path
          PKIXCertPathBuilderResult pathResult = (PKIXCertPathBuilderResult) builder.build(params);

          // Validate the entire chain
          validateCertificateValidity(pathResult.getCertPath(), result);

          result.addSuccess("O emissor do certificado é confiável");
          result.addSuccess("A raiz Autoridade Certificadora Raiz Brasileira v5 é confiável");

      } catch (Exception e) {
          result.addError("Error validating certificate chain: " + e.getMessage());
      }
  }

  private void validateCertificateValidity(CertPath certPath, ValidationResult result) {
      Date validationDate = new Date();
      for (Certificate cert : certPath.getCertificates()) {
          X509Certificate x509Cert = (X509Certificate) cert;
          try {
              x509Cert.checkValidity(validationDate);
              result.addSuccess("O certificado " + x509Cert.getSubjectX500Principal().getName() + 
                  " estava no período de validade em " + validationDate);
          } catch (CertificateExpiredException | CertificateNotYetValidException e) {
              result.addError("Certificate " + x509Cert.getSubjectX500Principal().getName() + 
                  " validity check failed: " + e.getMessage());
          }
      }
  }

  private X509Certificate loadBrazilianRootCertificate() throws Exception {
      try (InputStream is = getClass().getResourceAsStream("/certificates/ACRaizBrasileirav5.cer")) {
          if (is == null) {
              throw new FileNotFoundException("Brazilian root certificate not found in resources");
          }
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          return (X509Certificate) cf.generateCertificate(is);
      }
  }

  private X509CertificateHolder getCertificate(Store<X509CertificateHolder> certificates, 
                                              SignerId signerId) {
      try {
          @SuppressWarnings("unchecked")
          Collection<X509CertificateHolder> matches = certificates.getMatches(
              (Selector<X509CertificateHolder>) signerId);
          return matches.isEmpty() ? null : matches.iterator().next();
      } catch (Exception e) {
          return null;
      }
  }

  private void validateSignedAttributes(SignerInformation signer, ValidationResult result) {
      AttributeTable signedAttrs = signer.getSignedAttributes();
      if (signedAttrs == null) {
          result.addError("No signed attributes present");
          return;
      }

      // Check required attributes
      checkAttribute(signedAttrs, CMSAttributes.contentType, "Content Type", true, result);
      checkAttribute(signedAttrs, CMSAttributes.messageDigest, "Message Digest", true, result);
      checkAttribute(signedAttrs, PKCSObjectIdentifiers.id_aa_signingCertificate, 
          "Signing Certificate Reference", true, result);

      // Check for unknown attributes
      @SuppressWarnings("unchecked")
      Enumeration<ASN1ObjectIdentifier> attrTypes = signedAttrs.toHashtable().keys();
      while (attrTypes.hasMoreElements()) {
          ASN1ObjectIdentifier attrType = attrTypes.nextElement();
          if (!isKnownSignedAttribute(attrType)) {
              result.addError("Unknown signed attribute present: " + attrType);
              return;
          }
      }

      result.addSuccess("All required signed attributes are present and no unknown attributes found");
  }

  private void validateUnsignedAttributes(SignerInformation signer, ValidationResult result) {
      AttributeTable unsignedAttrs = signer.getUnsignedAttributes();

      if (unsignedAttrs == null) {
          result.addSuccess("No unsigned attributes present");
          return;
      }

      // Check forbidden attributes
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_certificateRefs,
          "complete-certificate-references", false, result);
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_revocationRefs,
          "complete-revocation-references", false, result);
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_escTimeStamp,
          "cades-c-timestamp", false, result);
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_certValues,
          "certificate-values", false, result);
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_revocationValues,
          "revocation-values", false, result);
      checkAttribute(unsignedAttrs, PKCSObjectIdentifiers.id_aa_ets_archiveTimestamp,
          "archive-timestamp", false, result);

      // Check for unknown attributes
      @SuppressWarnings("unchecked")
      Enumeration<ASN1ObjectIdentifier> attrTypes = unsignedAttrs.toHashtable().keys();
      while (attrTypes.hasMoreElements()) {
          ASN1ObjectIdentifier attrType = attrTypes.nextElement();
          if (!isKnownUnsignedAttribute(attrType)) {
              result.addError("Unknown unsigned attribute present: " + attrType);
              return;
          }
      }

      result.addSuccess("No forbidden or unknown unsigned attributes present");
  }

  private void checkAttribute(AttributeTable attrs, ASN1ObjectIdentifier oid, 
                            String attrName, boolean required, ValidationResult result) {
      boolean present = attrs.get(oid) != null;
      if (required && !present) {
          result.addError("Required attribute missing: " + attrName);
      } else if (!required && present) {
          result.addError("Forbidden attribute present: " + attrName);
      } else if (!required && !present) {
          result.addSuccess("The forbidden " + attrName + " attribute is not present");
      }
  }

  private boolean isKnownSignedAttribute(ASN1ObjectIdentifier oid) {
      return Arrays.asList(
          CMSAttributes.contentType,
          CMSAttributes.messageDigest,
          PKCSObjectIdentifiers.id_aa_signingCertificate,
          PKCSObjectIdentifiers.id_aa_signingCertificateV2,
          CMSAttributes.signingTime
      ).contains(oid);
  }

  private boolean isKnownUnsignedAttribute(ASN1ObjectIdentifier oid) {
      return Arrays.asList(
          PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
          PKCSObjectIdentifiers.id_aa_ets_certificateRefs,
          PKCSObjectIdentifiers.id_aa_ets_revocationRefs,
          PKCSObjectIdentifiers.id_aa_ets_certValues,
          PKCSObjectIdentifiers.id_aa_ets_revocationValues,
          PKCSObjectIdentifiers.id_aa_ets_escTimeStamp,
          PKCSObjectIdentifiers.id_aa_ets_archiveTimestamp
      ).contains(oid);
  }
}