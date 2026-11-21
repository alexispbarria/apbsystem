package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.helpers.Util;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Bean de sesión que centraliza la identidad del usuario autenticado,
 * la diferenciación de permisos/vistas entre Administrador (ADMIN) y Trabajador,
 * y utilidades globales para el Topbar y Plantilla Maestra.
 */
@Named("sessionBean")
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Permite a un usuario ADMIN real previsualizar cómo ve el sistema un Trabajador
     * durante la etapa de maqueta (sin alterar permisos reales de servidor).
     */
    private boolean vistaTrabajadorSimulada;

    private HttpSession getCurrentHttpSession() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getExternalContext() == null) {
            return null;
        }
        return (HttpSession) fc.getExternalContext().getSession(false);
    }

    public Users getUsuarioActual() {
        HttpSession session = getCurrentHttpSession();
        if (session == null) {
            return null;
        }
        Object obj = session.getAttribute("user");
        return (obj instanceof Users) ? (Users) obj : null;
    }

    public boolean isAutenticado() {
        return getUsuarioActual() != null;
    }

    /**
     * Indica si el usuario autenticado tiene privilegios reales de Administrador.
     */
    public boolean isRealAdmin() {
        HttpSession session = getCurrentHttpSession();
        if (session != null) {
            if (Boolean.TRUE.equals(session.getAttribute("es_admin"))
                    || Boolean.TRUE.equals(session.getAttribute("master"))) {
                return true;
            }
        }
        Users u = getUsuarioActual();
        if (u == null) {
            return false;
        }
        boolean flagAdmin = Boolean.TRUE.equals(u.getEsAdmin());
        boolean cargoAdmin = u.getCargos() != null
                && u.getCargos().getCodigo() != null
                && "ADMIN".equalsIgnoreCase(u.getCargos().getCodigo().trim());
        return flagAdmin || cargoAdmin;
    }

    /**
     * Controla si la interfaz actual debe renderizar menús y paneles de Administrador.
     */
    public boolean isAdminView() {
        return isRealAdmin() && !vistaTrabajadorSimulada;
    }

    /**
     * Controla si la interfaz actual debe renderizar la vista acotada de Trabajador.
     */
    public boolean isTrabajadorView() {
        return !isAdminView();
    }

    public boolean isVistaTrabajadorSimulada() {
        return isRealAdmin() && vistaTrabajadorSimulada;
    }

    public String alternarVistaTrabajador() {
        if (isRealAdmin()) {
            this.vistaTrabajadorSimulada = !this.vistaTrabajadorSimulada;
        }
        return redirigirVistaActual();
    }

    public String salirVistaSimulada() {
        this.vistaTrabajadorSimulada = false;
        return redirigirVistaActual();
    }

    private String redirigirVistaActual() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null && fc.getViewRoot() != null && fc.getViewRoot().getViewId() != null) {
            String viewId = fc.getViewRoot().getViewId();
            if (viewId.endsWith(".xhtml")) {
                viewId = viewId.substring(0, viewId.length() - 6) + ".hsm";
            }
            return viewId + "?faces-redirect=true";
        }
        return "/view/mailbox/index.hsm?faces-redirect=true";
    }

    public String getNombreMostrar() {
        Users u = getUsuarioActual();
        if (u != null) {
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                return u.getFullName().trim();
            }
            if (u.getUsername() != null && !u.getUsername().isBlank()) {
                return u.getUsername().trim();
            }
        }
        return "Usuario";
    }

    public String getPrimerNombre() {
        String nombre = getNombreMostrar();
        int idx = nombre.indexOf(' ');
        return idx > 0 ? nombre.substring(0, idx) : nombre;
    }

    public String getUsername() {
        Users u = getUsuarioActual();
        return (u != null && u.getUsername() != null) ? u.getUsername() : "";
    }

    public String getEmail() {
        Users u = getUsuarioActual();
        return (u != null && u.getEmail() != null) ? u.getEmail() : "";
    }

    public String getIniciales() {
        String nombre = getNombreMostrar();
        String[] partes = nombre.split("\\s+");
        if (partes.length >= 2) {
            return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
        }
        return nombre.length() >= 2 ? nombre.substring(0, 2).toUpperCase() : nombre.toUpperCase();
    }

    public String getRolCodigo() {
        if (isAdminView()) {
            return "ADMIN";
        }
        if (isVistaTrabajadorSimulada()) {
            return "TRABAJADOR";
        }
        HttpSession session = getCurrentHttpSession();
        if (session != null && session.getAttribute("rol_codigo") != null) {
            return String.valueOf(session.getAttribute("rol_codigo"));
        }
        Users u = getUsuarioActual();
        if (u != null && u.getCargos() != null && u.getCargos().getCodigo() != null) {
            return u.getCargos().getCodigo().trim().toUpperCase();
        }
        return "TRABAJADOR";
    }

    public String getRolEtiqueta() {
        if (isAdminView()) {
            return "Administrador";
        }
        if (isVistaTrabajadorSimulada()) {
            return "Trabajador";
        }
        HttpSession session = getCurrentHttpSession();
        if (session != null && session.getAttribute("rol_nombre") != null) {
            return String.valueOf(session.getAttribute("rol_nombre"));
        }
        return "CAJERO".equalsIgnoreCase(getRolCodigo()) ? "Cajero" : "Trabajador";
    }

    public String getRolBadgeClass() {
        return isAdminView() ? "apb-role-admin" : "apb-role-worker";
    }

    public String getRolIconClass() {
        return isAdminView() ? "pi pi-shield" : "pi pi-id-card";
    }

    public String getSessionUuidCorto() {
        HttpSession session = getCurrentHttpSession();
        if (session == null || session.getAttribute("session_uuid") == null) {
            return "LOCAL";
        }
        String uuid = String.valueOf(session.getAttribute("session_uuid"));
        return uuid.length() >= 8 ? uuid.substring(0, 8).toUpperCase() : uuid.toUpperCase();
    }

    /**
     * Acción compartida para el menú dropdown y accesos de módulos cuya tabla de BD
     * aún está en fase de diseño.
     */
    public void notificarModuloEnDesarrollo(String nombreModulo) {
        Util.avisoInfo(null, nombreModulo + ": módulo en etapa de maqueta (pendiente de creación de tablas en BD).");
    }
}
