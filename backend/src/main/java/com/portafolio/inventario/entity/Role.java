package com.portafolio.inventario.entity;

/** ADMIN configura el sistema y usuarios; WAREHOUSE registra movimientos; VIEWER solo consulta. */
public enum Role {
    ADMIN, WAREHOUSE, VIEWER
}
