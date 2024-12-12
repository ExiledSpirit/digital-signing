package com.example.demo.services;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class CertificateValidationService {
    public CertificateValidationService() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    
}
