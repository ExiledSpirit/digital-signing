package com.example.demo.controllers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.response.StartSigningResponse;
import com.example.demo.services.SignatureService;
import com.example.demo.session.SigningSession;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("remote-signing")
public class RemoteSigning {
    private final SigningSession signingSession;

    private final SignatureService signatureService;

    @Autowired
    public RemoteSigning(SigningSession signingSession, SignatureService signatureService) {
        this.signingSession = signingSession;
        this.signatureService = signatureService;
    }

    @PostMapping("start")
    public ResponseEntity<StartSigningResponse> start(
        @RequestPart MultipartFile file,
        @RequestParam String certContent,
        HttpSession session) throws IOException, GeneralSecurityException {
        return ResponseEntity.ok(this.signatureService.startRemoteSigning(file.getBytes(), certContent));
    }

    @PostMapping("complete")
    public ResponseEntity<byte[]> complete(@RequestParam String signedHash, HttpSession session)
        throws IOException, GeneralSecurityException {

        byte[] preparedPdfBytes = this.signingSession.getPreparedPdfBytes().readAllBytes();
        String fieldName = this.signingSession.getFieldName();
        String certCont = this.signingSession.getCertContent();
        byte[] beforeAttrBytes = this.signingSession.getBeforeAttr().readAllBytes();

        Assert.notNull(preparedPdfBytes, "Prepared PDF bytes not found in session");
        Assert.notNull(fieldName, "Field name not found in session");
        Assert.notNull(certCont, "Cert content not found in session");
        Assert.notNull(certCont, "Before attribute container bytes not found in session");
        
        byte[] clientSignatureBytes = Base64.getDecoder().decode(signedHash);

        byte[] signedPdf = this.signatureService.completeRemoteSigning(clientSignatureBytes, preparedPdfBytes, fieldName, beforeAttrBytes, certCont);

        session.invalidate();
        return ResponseEntity.ok(signedPdf);
    }

}
