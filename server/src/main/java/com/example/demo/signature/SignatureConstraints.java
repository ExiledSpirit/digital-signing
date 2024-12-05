package com.example.demo.signature;

import com.itextpdf.signatures.DigestAlgorithms;

public class SignatureConstraints {
    public static String DIGEST_ALGORITHM = "RSA";
    public static String HASH_ALGORITHM = DigestAlgorithms.SHA256;
    public static String FIELD_NAME = "CustomSignature";
    public static int EXTERNAL_CONTAINER_ESTIMATED_SIZE = 8192;
}
