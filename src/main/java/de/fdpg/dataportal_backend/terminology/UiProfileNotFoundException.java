package de.fdpg.dataportal_backend.terminology;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class UiProfileNotFoundException extends RuntimeException {

  public UiProfileNotFoundException() {
  }
}
