package com.hcg.multiservicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** DNI peruano: exactamente 8 digitos. Vacio/null se considera valido. */
@Documented
@Constraint(validatedBy = DniValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dni {
    String message() default "DNI no válido (8 dígitos)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
