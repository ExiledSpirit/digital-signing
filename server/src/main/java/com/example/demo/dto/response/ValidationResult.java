package com.example.demo.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResult {
  private List<String> successes = new ArrayList<>();
  private List<String> errors = new ArrayList<>();

  public void addSuccess(String message) {
      successes.add("Sucesso (" + message + ")");
  }

  public void addError(String message) {
      errors.add("Erro: " + message);
  }

  public boolean isValid() {
      return errors.isEmpty();
  }
}
