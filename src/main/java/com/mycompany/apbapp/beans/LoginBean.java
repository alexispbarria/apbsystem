package com.mycompany.apbapp.beans;

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
import java.util.Map;
import static java.util.UUID.randomUUID;

import org.mindrot.jbcrypt.BCrypt;

@RequestScoped
@Named("loginBean")
public class LoginBean implements Serializable {

    private static final String URL_HOME = "/view/mailbox/index.hsm?faces-redirect=true";
    private static String URL_LOGIN = "/login.hsm?faces-redirect=true";
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    @Inject
    private UsersController usersController;

    @Inject
    private LoginSessionController loginSessionController;

    private Users userValidado;

    private Users user;

    @PostConstruct
    public void init() {

        this.user = new Users();

        if (Util.getURLReal().equals("http://localhost:8080")) {

        }

    }

    public String login() throws Exception {
        if ((this.user.getUsername().isBlank() || this.user.getUsername() == null)
                && (this.user.getPassword().isBlank() || this.user.getPassword() == null)) {
            return null;
        }

        return this.loginClassic();
    }

    public String loginClassic() throws Exception {

        boolean redirect = false;
        this.userValidado = usersController.findByUsername(this.user);

        if (this.userValidado != null && this.userValidado.getEsAdmin()) {

            if (BCrypt.checkpw(this.user.getPassword(), this.userValidado.getPassword())) {

                // if (this.sgaUserValidado.getActivo() == 1) {
                HttpSession httpSession = Util.getSession();

                httpSession.setAttribute("user", userValidado);
                httpSession.setAttribute("full_name", userValidado.getFullName());
                httpSession.setAttribute("user_id", userValidado.getId());

                httpSession.setAttribute("autologin", false);
                httpSession.setAttribute("master", false);
                httpSession.setAttribute("vista", false);

                String session_uuid = randomUUID().toString();
                httpSession.setAttribute("session_uuid", session_uuid);
                httpSession.setAttribute("workDir", this.mkWorkDir(userValidado));

                if (this.userValidado.getCargos().getCodigo().equals("ADMIN")) {

                    httpSession.setAttribute("user_fullname", "(Master) " + userValidado.getFullName());
                    httpSession.setAttribute("master", true);
                    httpSession.setAttribute("user_master", userValidado);
                }

                loginSessionController.registrarLogin(Util.getURLReal(), session_uuid, userValidado);
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

//            if (autologin) {
//
//                if (Util.getURLReal().equals("http://localhost:8080")) {
//                    externalContext.redirect("http://localhost:8080/sistemas");
//                } else {
//                    externalContext.redirect(ConfigManager.GetProperty("url_base_sistemas") + "/sistemas/view/mailbox/index.hsm");
//                }
//                return;
//            }

            externalContext.redirect(externalContext.getRequestContextPath() + URL_LOGIN);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String mkWorkDir(Users userValidado) {

        String userName = this.userValidado.getEmail().toLowerCase();

        int index = userName.indexOf('@');

        userName = userName.substring(0, index);

        String workDir = TEMP_DIR + "/" + userName + "_" + randomUUID().toString().substring(0, 18);

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

                Util.avisoError("infoMsg", "Para poder ver este recurso, favor de iniciar sesión");
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
    
    

}
