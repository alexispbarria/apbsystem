package com.mycompany.apbapp.exceptions;

import jakarta.ejb.ApplicationException;

/**
 * Excepción de reglas de negocio que revierte automáticamente la transacción JTA
 * y entrega un mensaje limpio a la interfaz sin envolver en EJBException.
 */
@ApplicationException(rollback = true)
public class NegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public NegocioException(String message) {
        super(message);
    }

    public NegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}
