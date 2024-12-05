package com.example.demo.dto.response;

import lombok.Data;

@Data
public class StartSigningResponse {
  private String preparedPdfBytes;
  private String toSignHash;
}
