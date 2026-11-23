package com.portafolio.inventario.exception;

/** La petición es válida en forma pero viola una regla de negocio (p. ej. stock insuficiente). */
public class BusinessRuleException extends RuntimeException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
