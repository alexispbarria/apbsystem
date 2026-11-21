package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.CargoController;
import com.mycompany.apbapp.controllers.UsersController;
import com.mycompany.apbapp.entities.Cargos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.primefaces.PrimeFaces;

/**
 * Bean de vista exclusivo para Gestión de Usuarios y Cargos (/view/admin/usuarios.hsm).
 * Permite al Administrador crear cuentas de trabajadores/administradores, editarlas,
 * activar/desactivar accesos, restablecer contraseñas y crear nuevos cargos.
 */
@Named("usuarioBean")
@ViewScoped
public class UsuarioBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UsersController usersController;

    @Inject
    private CargoController cargoController;

    @Inject
    private SessionBean sessionBean;

    private List<Users> usuarios = new ArrayList<>();
    private List<Cargos> cargos = new ArrayList<>();

    // Filtros
    private String filtroTexto;
    private Integer filtroCargoId;
    private String filtroEstado = "TODOS";

    // Modal Crear/Editar Usuario
    private Users usuarioForm = new Users();
    private Integer cargoIdSeleccionado;
    private String passwordNueva;
    private String passwordConfirmar;

    // Modal Restablecer Contraseña
    private Users usuarioReset;
    private String resetPasswordNueva;
    private String resetPasswordConfirmar;

    // Modal Nuevo Cargo
    private Cargos cargoForm = new Cargos();

    @PostConstruct
    public void init() {
        cargarCargos();
        buscar();
    }

    public void cargarCargos() {
        this.cargos = cargoController.findActivos();
    }

    public void buscar() {
        this.usuarios = usersController.buscar(this.filtroTexto, this.filtroCargoId, this.filtroEstado);
    }

    public void limpiarFiltros() {
        this.filtroTexto = null;
        this.filtroCargoId = null;
        this.filtroEstado = "TODOS";
        cargarCargos();
        buscar();
    }

    public void abrirNuevoUsuario() {
        this.usuarioForm = new Users();
        this.usuarioForm.setEnabled(true);
        this.passwordNueva = null;
        this.passwordConfirmar = null;
        this.cargoIdSeleccionado = seleccionarCargoPorDefectoTrabajador();
    }

    private Integer seleccionarCargoPorDefectoTrabajador() {
        for (Cargos c : this.cargos) {
            if ("TRABAJADOR".equalsIgnoreCase(c.getCodigo())) {
                return c.getId();
            }
        }
        return !this.cargos.isEmpty() ? this.cargos.get(0).getId() : null;
    }

    public void abrirEditarUsuario(Users u) {
        if (u == null) {
            return;
        }
        this.usuarioForm = new Users();
        this.usuarioForm.setId(u.getId());
        this.usuarioForm.setFullName(u.getFullName());
        this.usuarioForm.setUsername(u.getUsername());
        this.usuarioForm.setEmail(u.getEmail());
        this.usuarioForm.setRut(u.getRut());
        this.usuarioForm.setEnabled(u.getEnabled());
        this.usuarioForm.setEsAdmin(u.getEsAdmin());
        this.cargoIdSeleccionado = u.getCargos() != null ? u.getCargos().getId() : null;
        this.passwordNueva = null;
        this.passwordConfirmar = null;
    }

    public void guardarUsuario() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede gestionar cuentas de usuarios.");
            return;
        }

        boolean esNuevo = (this.usuarioForm.getId() == null);
        if (esNuevo || (this.passwordNueva != null && !this.passwordNueva.isBlank())) {
            if (this.passwordNueva == null || this.passwordNueva.length() < 6) {
                Util.avisoError(null, "La contraseña debe tener al menos 6 caracteres.");
                return;
            }
            if (!this.passwordNueva.equals(this.passwordConfirmar)) {
                Util.avisoError(null, "Las contraseñas ingresadas no coinciden.");
                return;
            }
        }

        try {
            Users guardado = usersController.guardarUsuario(
                    this.usuarioForm,
                    this.cargoIdSeleccionado,
                    this.passwordNueva,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            this.passwordNueva = null;
            this.passwordConfirmar = null;
            buscar();
            Util.avisoInfo(null, esNuevo
                    ? "Cuenta @" + guardado.getUsername() + " creada correctamente."
                    : "Cuenta @" + guardado.getUsername() + " actualizada correctamente.");
            PrimeFaces.current().executeScript("PF('dlgUsuario').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void abrirResetPassword(Users u) {
        this.usuarioReset = u;
        this.resetPasswordNueva = null;
        this.resetPasswordConfirmar = null;
    }

    public void confirmarResetPassword() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Acción exclusiva para el Administrador.");
            return;
        }
        if (this.usuarioReset == null) {
            return;
        }
        if (this.resetPasswordNueva == null || this.resetPasswordNueva.length() < 6) {
            Util.avisoError(null, "La nueva contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if (!this.resetPasswordNueva.equals(this.resetPasswordConfirmar)) {
            Util.avisoError(null, "Las contraseñas no coinciden.");
            return;
        }

        try {
            usersController.restablecerPassword(
                    this.usuarioReset.getId(),
                    this.resetPasswordNueva,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            this.resetPasswordNueva = null;
            this.resetPasswordConfirmar = null;
            Util.avisoInfo(null, "Contraseña restablecida para @" + this.usuarioReset.getUsername() + ".");
            PrimeFaces.current().executeScript("PF('dlgResetPassword').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void alternarEstadoUsuario(Users u) {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Acción exclusiva para el Administrador.");
            return;
        }
        if (u == null || u.getId() == null) {
            return;
        }
        try {
            boolean nuevoEstado = !u.getEnabled();
            Users actualizado = usersController.cambiarEstado(
                    u.getId(),
                    nuevoEstado,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            buscar();
            Util.avisoInfo(null, nuevoEstado
                    ? "Cuenta @" + actualizado.getUsername() + " activada."
                    : "Cuenta @" + actualizado.getUsername() + " desactivada.");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void abrirNuevoCargo() {
        this.cargoForm = new Cargos();
        this.cargoForm.setEnabled(true);
    }

    public void guardarCargo() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede crear cargos.");
            return;
        }
        try {
            Cargos c = cargoController.guardarCargo(
                    this.cargoForm,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            cargarCargos();
            Util.avisoInfo(null, "Cargo '" + c.getNombre() + "' [" + c.getCodigo() + "] creado correctamente.");
            PrimeFaces.current().executeScript("PF('dlgCargo').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    // ---- Helpers y KPIs ----

    public boolean isEsUsuarioActual(Users u) {
        return u != null && u.getId() != null
                && sessionBean.getUsuarioActual() != null
                && u.getId().equals(sessionBean.getUsuarioActual().getId());
    }

    public boolean isCuentaAdmin(Users u) {
        if (u == null) {
            return false;
        }
        return u.getEsAdmin() || (u.getCargos() != null && "ADMIN".equalsIgnoreCase(u.getCargos().getCodigo()));
    }

    public String getInicialesUsuario(Users u) {
        if (u == null) {
            return "U";
        }
        String nombre = (u.getFullName() != null && !u.getFullName().isBlank())
                ? u.getFullName().trim()
                : (u.getUsername() != null ? u.getUsername().trim() : "U");
        String[] partes = nombre.split("\\s+");
        if (partes.length >= 2) {
            return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
        }
        return nombre.length() >= 2 ? nombre.substring(0, 2).toUpperCase() : nombre.toUpperCase();
    }

    public int getTotalUsuarios() {
        return this.usuarios.size();
    }

    public int getTotalActivos() {
        int c = 0;
        for (Users u : this.usuarios) {
            if (u.getEnabled()) {
                c++;
            }
        }
        return c;
    }

    public int getTotalAdmins() {
        int c = 0;
        for (Users u : this.usuarios) {
            if (isCuentaAdmin(u)) {
                c++;
            }
        }
        return c;
    }

    public int getTotalTrabajadores() {
        return getTotalUsuarios() - getTotalAdmins();
    }

    // ---- Getters & Setters ----

    public List<Users> getUsuarios() {
        return usuarios;
    }

    public List<Cargos> getCargos() {
        return cargos;
    }

    public String getFiltroTexto() {
        return filtroTexto;
    }

    public void setFiltroTexto(String filtroTexto) {
        this.filtroTexto = filtroTexto;
    }

    public Integer getFiltroCargoId() {
        return filtroCargoId;
    }

    public void setFiltroCargoId(Integer filtroCargoId) {
        this.filtroCargoId = filtroCargoId;
    }

    public String getFiltroEstado() {
        return filtroEstado;
    }

    public void setFiltroEstado(String filtroEstado) {
        this.filtroEstado = filtroEstado;
    }

    public Users getUsuarioForm() {
        return usuarioForm;
    }

    public void setUsuarioForm(Users usuarioForm) {
        this.usuarioForm = usuarioForm;
    }

    public Integer getCargoIdSeleccionado() {
        return cargoIdSeleccionado;
    }

    public void setCargoIdSeleccionado(Integer cargoIdSeleccionado) {
        this.cargoIdSeleccionado = cargoIdSeleccionado;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    public String getPasswordConfirmar() {
        return passwordConfirmar;
    }

    public void setPasswordConfirmar(String passwordConfirmar) {
        this.passwordConfirmar = passwordConfirmar;
    }

    public Users getUsuarioReset() {
        return usuarioReset;
    }

    public String getResetPasswordNueva() {
        return resetPasswordNueva;
    }

    public void setResetPasswordNueva(String resetPasswordNueva) {
        this.resetPasswordNueva = resetPasswordNueva;
    }

    public String getResetPasswordConfirmar() {
        return resetPasswordConfirmar;
    }

    public void setResetPasswordConfirmar(String resetPasswordConfirmar) {
        this.resetPasswordConfirmar = resetPasswordConfirmar;
    }

    public Cargos getCargoForm() {
        return cargoForm;
    }

    public void setCargoForm(Cargos cargoForm) {
        this.cargoForm = cargoForm;
    }
}
