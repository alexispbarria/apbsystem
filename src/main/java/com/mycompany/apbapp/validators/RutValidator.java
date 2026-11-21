package com.mycompany.apbapp.validators;

import com.mycompany.apbapp.helpers.Util;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

/**
 * Validador JSF reutilizable para RUT chileno bajo la convención del sistema: xxxxxxxx-x.
 * Uso en XHTML:
 *   <p:inputText value="#{bean.rut}" converter="rutConverter">
 *       <f:validator validatorId="rutValidator"/>
 *   </p:inputText>
 */
@FacesValidator("rutValidator")
public class RutValidator implements Validator<String> {

    @Override
    public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
        if (value == null || value.isBlank()) {
            return; // La obligatoriedad se controla con required="true"
        }

        String rut = value.trim().toUpperCase();

        if (!Util.tieneFormatoRutConvencion(rut)) {
            throw new ValidatorException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Formato de RUT inválido",
                    "Usa el formato xxxxxxxx-x sin puntos (ej: 12345678-5)."
            ));
        }

        if (!Util.validarRut(rut)) {
            throw new ValidatorException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "RUT no válido",
                    "El dígito verificador no corresponde al número ingresado."
            ));
        }
    }
}
