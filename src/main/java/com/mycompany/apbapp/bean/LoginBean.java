package com.mycompany.apbapp.bean;

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
import java.util.Map;
import static java.util.UUID.randomUUID;

import org.springframework.security.crypto.bcrypt.BCrypt;

@RequestScoped
@Named("loginBean")
public class LoginBean implements Serializable {

    private static final String URL_HOME = "/view/mailbox/index.hsm?faces-redirect=true";
    private static String URL_LOGIN = "/login.hsm?faces-redirect=true";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Inject
    private SGAUsersController sgaUsersController;

    @Inject
    private SgaLoginSessionController sgaLoginSessionController;

    private SgaUsers sgaUserValidado;

    private SgaUsers sgaUser;

    public SgaUsers getSgaUserValidado() {
        return sgaUserValidado;
    }

    public void setSgaUserValidado(SgaUsers sgaUserValidado) {
        this.sgaUserValidado = sgaUserValidado;
    }

    public SgaUsers getSgaUser() {
        return sgaUser;
    }

    public void setSgaUser(SgaUsers sgaUser) {
        this.sgaUser = sgaUser;
    }

    @PostConstruct
    public void init() {

        this.sgaUser = new SgaUsers();

        if (Util.getURLReal().equals("http://localhost:8080")) {

        }

    }

    public String login() throws Exception {
        if ((this.sgaUser.getUsername().isBlank() || this.sgaUser.getUsername() == null)
                && (this.sgaUser.getPassword().isBlank() || this.sgaUser.getPassword() == null)) {
            return null;
        }

        return this.loginClassic();
    }

    public String loginClassic() throws Exception {

        boolean redirect = false;
        this.sgaUserValidado = sgaUsersController.findByUsername(this.sgaUser);
        System.out.println("/**************************CORREOOOO validado /" + sgaUserValidado.getEmail());

        boolean sgaGrupoPermitido = false;
        boolean sgaDivisionPermitida = false;
        boolean sgaUserMasterPermitido = false;
        boolean sgaUserVistaPermitido = false;

        if (this.sgaUserValidado != null) {
            String codigo = this.sgaUserValidado.getSgaGroups().getCodigo();
            sgaGrupoPermitido = codigo.equals("ABASTECIMIENTO")
                    || codigo.equals("SILVANA_TORRES_MEJIAS");

//            String division = this.sgaUserValidado.getSgaDivisiones().getCodigo();
//            sgaDivisionPermitida = division.equals("ADMINISTRACION_FINANZAS");

            String userMaster = this.sgaUserValidado.getUsername();
            sgaUserMasterPermitido = userMaster.equals("storres")
                    || userMaster.equals("desarrollo")
                    || userMaster.equals("nahumada");

            String userVista = this.sgaUserValidado.getUsername();
            sgaUserVistaPermitido = userVista.equals("karza")
                    || userVista.equals("ffuentes")
                    || userVista.equals("iperezdearce");
        }

        if (this.sgaUserValidado != null && (this.sgaUserValidado.getManager()
                || sgaUserMasterPermitido || sgaUserVistaPermitido)) {

            if (BCrypt.checkpw(this.sgaUser.getPassword(), this.sgaUserValidado.getPassword())) {

                // if (this.sgaUserValidado.getActivo() == 1) {
                HttpSession httpSession = Util.getSession();

                httpSession.setAttribute("user", sgaUserValidado);
                httpSession.setAttribute("full_name", sgaUserValidado.getFullName());
                httpSession.setAttribute("user_id", sgaUserValidado.getId());

                httpSession.setAttribute("autologin", false);
                httpSession.setAttribute("master", false);
                httpSession.setAttribute("vista", false);

                String session_uuid = randomUUID().toString();
                httpSession.setAttribute("session_uuid", session_uuid);
                httpSession.setAttribute("workDir", this.mkWorkDir(sgaUserValidado));

                if (this.sgaUserValidado.getUsername().equals("desarrollo")
                        || this.sgaUserValidado.getUsername().equals("storres")
                        || this.sgaUserValidado.getUsername().equals("nahumada")) {

                    httpSession.setAttribute("user_fullname", "(Master) " + sgaUserValidado.getFullName());
                    httpSession.setAttribute("master", true);
                    httpSession.setAttribute("user_master", sgaUserValidado);
                }

                if (this.sgaUserValidado.getUsername().equals("karza")
                        || this.sgaUserValidado.getUsername().equals("ffuentes")
                        || this.sgaUserValidado.getUsername().equals("iperezdearce")) {

                    httpSession.setAttribute("user_vista_fullname", "(Vista) " + sgaUserValidado.getFullName());
                    httpSession.setAttribute("vista", true);
                    httpSession.setAttribute("user_vista", sgaUserValidado);
                }

                if (this.sgaUserValidado.getSgaGroups() != null) {
                    httpSession.setAttribute("group_id", this.sgaUserValidado.getSgaGroups().getId());
                    httpSession.setAttribute("group_code", this.sgaUserValidado.getSgaGroups().getCodigo());
                }

                sgaLoginSessionController.registrarLogin(Util.getURLRealAll(), session_uuid, sgaUserValidado);
                redirect = true;

//                } else {
//                    Util.avisoError("infoMsg", "Usuario desactivado");
//                }
            } else {
                Util.avisoError("infoMsg", "Usuario o contraseña incorrecta");
            }
        } else {
            Util.avisoError("infoMsg", "Usuario o contraseña incorrecta");
        }

        if (redirect) {
            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            Cookie cookie = new Cookie("omega_expandeditems", "menuform%3Aom_home");
            cookie.setPath("/");
            response.addCookie(cookie);
            return URL_HOME;
        }

        return null;
    }

    public void logout() {
        try {
            HttpSession httpSession = Util.getSession();
            boolean autologin = (boolean) httpSession.getAttribute("autologin");
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            httpSession.invalidate();

            if (autologin) {

                if (Util.getURLReal().equals("http://localhost:8080")) {
                    externalContext.redirect("http://localhost:8080/sistemas");
                } else {
                    externalContext.redirect(ConfigManager.GetProperty("url_base_sistemas") + "/sistemas/view/mailbox/index.hsm");
                }
                return;
            }

            externalContext.redirect(externalContext.getRequestContextPath() + URL_LOGIN);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
