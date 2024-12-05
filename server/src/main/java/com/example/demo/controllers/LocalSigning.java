package com.example.demo.controllers;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.services.SignatureService;

@RequestMapping("local-signing")
@RestController
public class LocalSigning {
    private final SignatureService signatureService;
    private static final char[] password = "123456".toCharArray();

    public LocalSigning(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @GetMapping("sign")
    public ResponseEntity<byte[]> sign(@RequestPart MultipartFile file) throws IOException, GeneralSecurityException {
        byte[] signedPdf = this.signatureService.locallySign(file.getBytes(), password);
        return ResponseEntity.ok(signedPdf);
    }
}
