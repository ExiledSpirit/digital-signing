package com.example.demo.session;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.StreamSerializer;

import lombok.Data;

/**
 * This class serve as a signing state to preserve data between the two
 * remote signing process steps. If some property has a specific usability then
 * it is described in its comment.
 */
@Data
@Component
@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SigningSession implements Serializable {
    /**
     * The bytes of the original PDF (before adding any signature container).
     */
    @JsonSerialize(using=StreamSerializer.class, as=byte[].class)
     private ByteArrayInputStream preparedPdfBytes;

    /**
     * The field name defined in the 1st remote signing step.
     */
    private String fieldName;

    /**
     * Certificate in base64. Can be decoded using java.util.Base64 and
     * java.security.cert.CertificateFactory utilitary classes.
     */
    private String certContent;

    /**
     * Its the bytes generated from pkcs7.getAuthenticatedAttributeBytes.
     * This is currently only being used for debugging purposes.
     */
    @JsonSerialize(using=StreamSerializer.class, as=byte[].class)
     private ByteArrayInputStream toSign;

    /**
     * Short term for Before Attribute Bytes. Its used to store
     * the bytes of the container before we apply the pkcs7 signature. Its
     * created in the 1st remote signing step and used in the complete step
     * to apply the signature to.
     */
    @JsonSerialize(using=StreamSerializer.class, as=byte[].class)
     private ByteArrayInputStream beforeAttr;
}
