package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.AuditLogController;
import com.mycompany.apbapp.controllers.LoginSessionController;
import com.mycompany.apbapp.controllers.UsersController;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.helpers.Util;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static java.util.UUID.randomUUID;

import org.mindrot.jbcrypt.BCrypt;
import org.primefaces.PrimeFaces;

@RequestScoped
@Named("loginBean")
public class LoginBean implements Serializable {

    private static final String URL_HOME = "/view/mailbox/index.hsm?faces-redirect=true";
    private static String URL_LOGIN = "/login.hsm?faces-redirect=true";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String COOKIE_REMEMBER_USER = "apb_remember_user";

    @Inject
    private UsersController usersController;

    @Inject
    private LoginSessionController loginSessionController;

    @Inject
    private AuditLogController auditLogController;

    private Users userValidado;

    private Users user;

    private boolean rememberMe;

    private String recoveryIdentifier;

    private boolean recoverySent;

    @PostConstruct
    public void init() {

        this.user = new Users();

        // Prellenar solo el nombre de usuario si el usuario activo "Recordar usuario"
        if (FacesContext.getCurrentInstance() != null) {
            Cookie remembered = Util.getCookie(COOKIE_REMEMBER_USER);
            if (remembered != null && remembered.getValue() != null && !remembered.getValue().isBlank()) {
                this.user.setUsername(URLDecoder.decode(remembered.getValue(), StandardCharsets.UTF_8));
                this.rememberMe = true;
            }
        }

        if (FacesContext.getCurrentInstance() != null && Util.getURLReal().equals("http://localhost:8080")) {

        }

    }

    public String login() throws Exception {
        if (this.user == null
                || this.user.getUsername() == null || this.user.getUsername().isBlank()
                || this.user.getPassword() == null || this.user.getPassword().isBlank()) {
            Util.avisoError(null, "Debes ingresar tu usuario y contraseña");
            PrimeFaces.current().ajax().addCallbackParam("loginFailed", true);
            return null;
        }

        return this.loginClassic();
    }

    public String loginClassic() throws Exception {

        boolean redirect = false;
        this.userValidado = usersController.findByUsername(this.user);

        if (this.userValidado != null && this.verificarPassword(this.user.getPassword(), this.userValidado.getPassword())) {

            String cargoCodigo = (this.userValidado.getCargos() != null && this.userValidado.getCargos().getCodigo() != null)
                    ? this.userValidado.getCargos().getCodigo().trim().toUpperCase()
                    : "";
            boolean esAdmin = Boolean.TRUE.equals(this.userValidado.getEsAdmin()) || "ADMIN".equals(cargoCodigo);
            String rolCodigo = esAdmin ? "ADMIN" : (cargoCodigo.isEmpty() ? "TRABAJADOR" : cargoCodigo);
            String rolNombre = esAdmin ? "Administrador" : ("CAJERO".equals(rolCodigo) ? "Cajero" : "Trabajador");

            if (!this.userValidado.getEnabled()) {
                auditLogController.registrar(
                        userValidado,
                        rolCodigo,
                        "AUTH",
                        "LOGIN_BLOQUEADO",
                        "Users",
                        userValidado.getId(),
                        "Intento de acceso con cuenta desactivada @" + userValidado.getUsername(),
                        Util.getRemoteAddr()
                );
                Util.avisoError(null, "Tu cuenta se encuentra desactivada. Contacta al Administrador.");
                PrimeFaces.current().ajax().addCallbackParam("loginFailed", true);
                return null;
            }

            HttpSession httpSession = Util.getSession();

            httpSession.setAttribute("user", userValidado);
            httpSession.setAttribute("full_name", userValidado.getFullName());
            httpSession.setAttribute("user_id", userValidado.getId());

            httpSession.setAttribute("autologin", false);
            httpSession.setAttribute("master", esAdmin);
            httpSession.setAttribute("es_admin", esAdmin);
            httpSession.setAttribute("rol_codigo", rolCodigo);
            httpSession.setAttribute("rol_nombre", rolNombre);
            httpSession.setAttribute("vista", false);

            String session_uuid = randomUUID().toString();
            httpSession.setAttribute("session_uuid", session_uuid);
            httpSession.setAttribute("workDir", this.mkWorkDir(userValidado));

            if (esAdmin) {
                httpSession.setAttribute("user_fullname", "(Master) " + userValidado.getFullName());
                httpSession.setAttribute("user_master", userValidado);
            } else {
                httpSession.setAttribute("user_fullname", userValidado.getFullName());
            }

            loginSessionController.registrarLogin(Util.getURLReal(), session_uuid, userValidado);
            auditLogController.registrar(
                    userValidado,
                    rolCodigo,
                    "AUTH",
                    "LOGIN",
                    "Users",
                    userValidado.getId(),
                    "Inicio de sesión exitoso (" + rolNombre + ")",
                    Util.getRemoteAddr()
            );
            redirect = true;

//            } else {
//                Util.avisoError(null, "Usuario desactivado");
//            }
        } else {
            Util.avisoError(null, "Usuario o contraseña incorrecta");
        }

        if (redirect) {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            Cookie cookie = new Cookie("omega_expandeditems", "menuform%3Aom_home");
            cookie.setPath("/");
            response.addCookie(cookie);

            this.actualizarCookieRecordarUsuario(response);
            return URL_HOME;
        }

        PrimeFaces.current().ajax().addCallbackParam("loginFailed", true);
        return null;
    }

    /**
     * Flujo maqueta de recuperacion de contraseña.
     * TODO: Conectar aqui el servicio real de validacion/envio SMTP o solicitud al administrador.
     */
    public void solicitarRecuperacion() {
        if (this.recoveryIdentifier == null || this.recoveryIdentifier.isBlank()) {
            Util.avisoError(null, "Ingresa tu usuario o correo corporativo");
            PrimeFaces.current().ajax().addCallbackParam("recoverySuccess", false);
            return;
        }

        this.recoveryIdentifier = this.recoveryIdentifier.trim();
        this.recoverySent = true;
        Util.avisoInfo(null, "Solicitud de recuperación registrada");
        PrimeFaces.current().ajax().addCallbackParam("recoverySuccess", true);
    }

    private void actualizarCookieRecordarUsuario(HttpServletResponse response) {
        if (this.rememberMe && this.user != null && this.user.getUsername() != null && !this.user.getUsername().isBlank()) {
            String encodedUser = URLEncoder.encode(this.user.getUsername().trim(), StandardCharsets.UTF_8);
            Cookie rememberCookie = new Cookie(COOKIE_REMEMBER_USER, encodedUser);
            rememberCookie.setPath("/");
            rememberCookie.setHttpOnly(true);
            rememberCookie.setMaxAge(60 * 60 * 24 * 30); // 30 dias
            response.addCookie(rememberCookie);
        } else {
            Cookie clearCookie = new Cookie(COOKIE_REMEMBER_USER, "");
            clearCookie.setPath("/");
            clearCookie.setHttpOnly(true);
            clearCookie.setMaxAge(0);
            response.addCookie(clearCookie);
        }
    }

    private boolean verificarPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void logout() {
        try {
            HttpSession httpSession = (HttpSession) FacesContext.getCurrentInstance()
                    .getExternalContext().getSession(false);
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            if (httpSession != null) {
                Object usrObj = httpSession.getAttribute("user");
                String rolCodigo = httpSession.getAttribute("rol_codigo") != null
                        ? String.valueOf(httpSession.getAttribute("rol_codigo"))
                        : "USUARIO";
                if (usrObj instanceof Users) {
                    Users u = (Users) usrObj;
                    auditLogController.registrar(
                            u,
                            rolCodigo,
                            "AUTH",
                            "LOGOUT",
                            "Users",
                            u.getId(),
                            "Cierre de sesión de usuario @" + u.getUsername(),
                            Util.getRemoteAddr()
                    );
                }
                httpSession.invalidate();
            }

            externalContext.redirect(externalContext.getRequestContextPath() + URL_LOGIN);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String mkWorkDir(Users userValidado) {

        String baseName = "user";
        if (userValidado != null) {
            if (userValidado.getEmail() != null && userValidado.getEmail().contains("@")) {
                String emailLower = userValidado.getEmail().toLowerCase().trim();
                baseName = emailLower.substring(0, emailLower.indexOf('@'));
            } else if (userValidado.getUsername() != null && !userValidado.getUsername().isBlank()) {
                baseName = userValidado.getUsername().toLowerCase().trim();
            }
        }
        baseName = baseName.replaceAll("[^a-z0-9._-]", "_");

        String workDir = TEMP_DIR + File.separator + baseName + "_" + randomUUID().toString().substring(0, 18);

        File file = new File(workDir);

        boolean bool = file.mkdir();

        if (bool) {

            System.out.println("Directorio creado con éxito");

        } else {

            System.out.println("No se pudo crear el directorio");
        }

        return workDir;
    }

    public void showMessage() {

        Map<String, String> paramMap = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();

        if (!paramMap.isEmpty()) {

            boolean error = Boolean.parseBoolean(paramMap.get("error"));

            if (error == true) {

                Util.avisoError(null, "Para poder ver este recurso, favor de iniciar sesión");
            }

        }

    }

    public Users getUserValidado() {
        return userValidado;
    }

    public void setUserValidado(Users userValidado) {
        this.userValidado = userValidado;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getRecoveryIdentifier() {
        return recoveryIdentifier;
    }

    public void setRecoveryIdentifier(String recoveryIdentifier) {
        this.recoveryIdentifier = recoveryIdentifier;
    }

    public boolean isRecoverySent() {
        return recoverySent;
    }

    public void setRecoverySent(boolean recoverySent) {
        this.recoverySent = recoverySent;
    }

}
