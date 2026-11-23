package com.portafolio.inventario.validation;

import com.portafolio.inventario.dto.StockMovementRequest;
import com.portafolio.inventario.entity.MovementType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidMovementValidator implements ConstraintValidator<ValidMovement, StockMovementRequest> {

    static final int MIN_ADJUSTMENT_REASON_LENGTH = 5;

    @Override
    public boolean isValid(StockMovementRequest request, ConstraintValidatorContext context) {
        if (request == null || request.type() != MovementType.ADJUSTMENT) {
            return true;
        }
        boolean valid = true;
        context.disableDefaultConstraintViolation();
        if (request.direction() == null) {
            context.buildConstraintViolationWithTemplate("Indica si el ajuste aumenta o disminuye el stock")
                    .addPropertyNode("direction").addConstraintViolation();
            valid = false;
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.length() < MIN_ADJUSTMENT_REASON_LENGTH) {
            context.buildConstraintViolationWithTemplate(
                            "Los ajustes deben explicar el motivo (mínimo " + MIN_ADJUSTMENT_REASON_LENGTH + " caracteres)")
                    .addPropertyNode("reason").addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
