package com.example.demo.controllers;

import com.example.demo.dto.response.ValidationResult;
import com.example.demo.services.ValidationService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/validation")
public class ValidationController {

  @Autowired
  private ValidationService validationService;

  @PostMapping("/validate")
  public ResponseEntity<ValidationResult> validateSignature(
          @RequestPart MultipartFile file) {
      try {
          ValidationResult result = validationService.validateSignature(file.getBytes());
          return ResponseEntity.ok(result);
      } catch (Exception e) {
          ValidationResult errorResult = new ValidationResult();
          errorResult.addError("Error processing request: " + e.getMessage());
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(errorResult);
      }
  }
}