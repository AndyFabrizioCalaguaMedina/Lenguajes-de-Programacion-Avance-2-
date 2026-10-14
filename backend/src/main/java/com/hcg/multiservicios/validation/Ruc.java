package com.hcg.multiservicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** RUC peruano: 11 digitos, prefijo valido y digito verificador correcto. Vacio/null se considera valido (use @NotBlank si es obligatorio). */
@Documented
@Constraint(validatedBy = RucValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ruc {
    String message() default "RUC no válido (11 dígitos y dígito verificador correcto)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
