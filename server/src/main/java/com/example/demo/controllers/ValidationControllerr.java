package com.example.demo.controllers;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.services.SignatureInformationService;
import com.example.demo.services.ValidationServicee;

@RequestMapping("validationn")
@RestController
public class ValidationControllerr {
    private final ValidationServicee validationService;

    private final SignatureInformationService signatureInformationService;

    public ValidationControllerr(ValidationServicee validationService, SignatureInformationService signatureInformationService) {
        this.validationService = validationService;
        this.signatureInformationService = signatureInformationService;
    }

    @PostMapping("validate-signatures")
    public ResponseEntity<Void> validateSignatures(@RequestPart MultipartFile file) throws IOException, GeneralSecurityException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        this.validationService.verifySignatures(file.getBytes());
        this.signatureInformationService.inspectSignatures(file.getBytes());
        return ResponseEntity.ok(null);
    }
}
