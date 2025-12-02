package de.numcodex.feasibility_gui_backend.query.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataExtractionValidator.class)
public @interface DataExtractionValidation {
  String message() default "DataExtraction is invalid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
