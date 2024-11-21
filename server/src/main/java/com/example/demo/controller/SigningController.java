package com.example.demo.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.request.SignatureCompleteRequest;
import com.example.demo.signaturecontainer.PreSignatureContainer;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignatureContainer;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;

@RestController
@RequestMapping("/signing")
public class SigningController {
  private static final String DIGEST_ALGORITHM = DigestAlgorithms.SHA256;

  /**
   * The first part of the signing process. This method receives the original PDF bytes (to be signed PDF)
   * and adds a blank signature (without signing hash), its necessary to do this server-side so we can have
   * more control over signature containers.
   * 
   * @param certContent
   * @param certThumb
   * @param file
   * @return
   * @throws Exception
   */
  @PostMapping("/start")
  public Map<String, byte[]> startSigning(@RequestParam String certContent, @RequestParam String certThumb, @RequestParam MultipartFile file) throws Exception {
      try {
        X509Certificate certificate = this.decodeCertificate(certContent);
        
        // Signed PDF Stream
        ByteArrayOutputStream signedPdfStream = new ByteArrayOutputStream();

        // Create PDF reader
        PdfReader reader = new PdfReader(new ByteArrayInputStream(file.getBytes())); // Your sample doc method
        
        // Create signer and stream
        PdfSigner signer = new PdfSigner(reader, signedPdfStream, new StampingProperties());

        // Configure appearance
        PdfSignatureAppearance sigAppearance = signer.getSignatureAppearance();
        sigAppearance.setReason("Certificação do Documento");
        sigAppearance.setLocation("Governo Digital");
        sigAppearance.setCertificate(certificate);
        // Signature appearance, not sure how, but should be a way to add more fancy styling to it
        sigAppearance.setPageRect(new Rectangle(72,632,200,100));
        
        // Set the field name so we can reference it in the next signing step.
        signer.setFieldName("certificatation");

        PdfPKCS7 sgn = new PdfPKCS7(null, new X509Certificate[]{certificate}, "SHA256", null, new BouncyCastleDigest(), false);
        // instantiate the signature container
        PreSignatureContainer external = new PreSignatureContainer(PdfName.Adobe_PPKLite, PdfName.Adbe_pkcs7_detached);
        
        // Prepare PDF for signing. I dont remember exactly why, but I think 8192 bytes is the max size of the padded signature container.
        signer.signExternalContainer(external, 8192);
        
        // Get the PDF file hash.
        byte[] hash = external.getHash();

        /**
         * Get the signature container bytes (we dont sign the whole document)
         * so we can give it back to our frontend client and generate the signing hash from it.
         **/ 
        byte[] sh = sgn.getAuthenticatedAttributeBytes(hash, PdfSigner.CryptoStandard.CADES, null, null);// sh will be sent for signature
        HashMap<String, byte[]> result = new HashMap<>();
        result.put("preparedPdfBytes", signedPdfStream.toByteArray());
        result.put("toSignHash", sh);
        return result;
      } catch (Exception ex) {
        return null;
      }
  }

  /**
   * The second part of the signing process. This method receives the prepared PDF bytes (result of the first signing step)
   * along with the  certificate and signed hash. This method is responsible for adding the signature hash to the
   * prepared PDF (with blank signature form) using the CAdES signing type.
   * 
   * @param preparedPdfBytes 
   * @param certContent
   * @param certThumb
   * @param signedHash
   * @return
   * @throws Exception
   */
  @PostMapping("/complete")
  public ResponseEntity<byte[]> completeSigning(
      @RequestParam MultipartFile preparedPdfBytes,
      @RequestParam String certContent,
      @RequestParam String certThumb,
      @RequestParam String signedHash) throws Exception {
      
      SignatureCompleteRequest request = new SignatureCompleteRequest(signedHash, preparedPdfBytes.getBytes(), certContent, certThumb);
      
      try {
          // Read the prepared PDF
          PdfReader reader = new PdfReader(new ByteArrayInputStream(request.getPreparedPdfBytes()));
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties());

        X509Certificate certificate = this.decodeCertificate(certContent);
          
          // Prepare PdfPKCS7 signature container
          PdfPKCS7 pkcs7 = new PdfPKCS7(null, new X509Certificate[]{certificate}, DIGEST_ALGORITHM, null, new BouncyCastleDigest(), false);
  
          // Decode client-signed hash
          byte[] clientSignedHash = Base64.getDecoder().decode(request.getSignedHash());
  
          // Complete the PKCS7 structure with the client-signed hash
          pkcs7.setExternalDigest(clientSignedHash, null, "RSA");
  
          // Generate the full PKCS7 signature
          byte[] encodedSig = pkcs7.getEncodedPKCS7(null, PdfSigner.CryptoStandard.CADES, null, null, null);
  
          // Create and set the external signature container
          IExternalSignatureContainer external = new IExternalSignatureContainer() {
              @Override
              public byte[] sign(InputStream data) {
                  return encodedSig; // Return the complete PKCS7 signature
              }
  
              @Override
              public void modifySigningDictionary(PdfDictionary signDic) {
                  signDic.put(PdfName.Filter, PdfName.Adobe_PPKLite);
                  signDic.put(PdfName.SubFilter, PdfName.Adbe_pkcs7_detached);
              }
          };
  
          signer.getSignatureAppearance().setCertificate(certificate);
          // Perform the deferred signing to avoid issues with byte ranges
          PdfSigner.signDeferred(signer.getDocument(), "certificatation", outputStream, external);
          signer.getDocument().close();
  
          // Return the signed PDF
          return ResponseEntity.ok()
              .contentType(MediaType.APPLICATION_PDF)
              .body(outputStream.toByteArray());
  
      } catch (Exception ex) {
          ex.printStackTrace();
          return ResponseEntity.badRequest().body(null);
      }
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
}
