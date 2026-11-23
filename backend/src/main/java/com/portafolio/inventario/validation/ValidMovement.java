package com.portafolio.inventario.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Reglas que dependen de varios campos del movimiento:
 * un ajuste debe indicar sentido (aumento/disminución) y explicar el motivo.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMovementValidator.class)
public @interface ValidMovement {
    String message() default "Movimiento inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
