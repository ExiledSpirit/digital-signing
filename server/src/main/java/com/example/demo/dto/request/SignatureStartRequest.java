package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The body request necessary to start the signing process through /start POST method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureStartRequest {
    /**
     * The original PDF bytes to be signed.
     */
    private byte[] originalPdf;

    /**
     * The Base64 SHA-256 certificate.
     */
    private String certContent;

    /**
     * The certificate thumbprint. Its not really being used in this sample code,
     * but could be used as an unique identifier so the server can store and retrieve the preparedPdfBytes,
     * kinda of simulating a session management. 
     */
    private String certThumb;
}