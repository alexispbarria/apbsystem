package com.mycompany.apbapp.validators;

import com.mycompany.apbapp.helpers.Util;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * Converter JSF reutilizable que normaliza automáticamente cualquier RUT ingresado
 * a la convención oficial del sistema: xxxxxxxx-x (sin puntos, DV en mayúscula).
 */
@FacesConverter("rutConverter")
public class RutConverter implements Converter<String> {

    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Util.formatearRUT(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String formatted = Util.formatearRUT(value);
        return formatted != null ? formatted : value;
    }
}
