package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The body request necessary to complete the signing process through /complete POST method.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureCompleteRequest {
    /** Signature Hash (created on client-side) */
    private String signedHash;

    /**
     * Prepared PDF bytes. It's basically the bytes returned after the /start POST request.
     * Ideally it should be stored in the server so the client doesn't need to provide it in /complete POST method.
     **/
    private byte[] preparedPdfBytes;
    
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