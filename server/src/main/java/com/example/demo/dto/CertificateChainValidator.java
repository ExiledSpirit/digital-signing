package com.example.demo.dto;

import java.io.InputStream;
import java.security.cert.CertPath;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;

import com.example.demo.dto.response.ValidationResult;
public class CertificateChainValidator {

  private X509Certificate loadBrazilianRootCertificate() throws Exception {
      // Load the Brazilian root certificate from resources
      try (InputStream is = getClass().getResourceAsStream("/certificates/ICP-Brasilv5.crt")) {
          CertificateFactory cf = CertificateFactory.getInstance("X.509");
          return (X509Certificate) cf.generateCertificate(is);
      }
  }

  private X509Certificate convertToX509Certificate(X509CertificateHolder certHolder) throws Exception {
      JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
      converter.setProvider(new BouncyCastleProvider());
      return converter.getCertificate(certHolder);
  }

  private CertStore convertToJavaCertStore(Store<X509CertificateHolder> certificates) throws Exception {
      List<X509Certificate> certList = new ArrayList<>();
      for (X509CertificateHolder certHolder : certificates.getMatches(null)) {
          certList.add(convertToX509Certificate(certHolder));
      }
      return CertStore.getInstance("Collection",
              new CollectionCertStoreParameters(certList),
              "BC");
  }

  private void validateCertificateValidity(CertPath certPath, ValidationResult result) {
      try {
          for (Certificate cert : certPath.getCertificates()) {
              X509Certificate x509Cert = (X509Certificate) cert;
              Date now = new Date();
              x509Cert.checkValidity(now);
              result.addSuccess("O certificado " + x509Cert.getSubjectX500Principal().getName() + 
                  " estava no período de validade em " + now);
          }
      } catch (CertificateExpiredException | CertificateNotYetValidException e) {
          result.addError("Certificate validity check failed: " + e.getMessage());
      }
  }
}