package com.example.demo.signature;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public interface Pkcs7Signer {
  byte[] sign(byte[] data, PrivateKey privateKey, Certificate[] certificateChain) throws GeneralSecurityException;
}
