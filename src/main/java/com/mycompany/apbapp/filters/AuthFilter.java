package com.mycompany.apbapp.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filtro de seguridad para todas las vistas internas bajo /view/*.
 * - Bloquea acceso sin sesión activa redirigiendo a /login.hsm?error=true.
 * - Restringe rutas bajo /view/admin/* exclusivamente a usuarios Administradores.
 * - Soporta redirección segura en solicitudes AJAX parciales de PrimeFaces/JSF.
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/view/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();

        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        if (!loggedIn) {
            redirectSafely(req, res, contextPath + "/login.hsm?error=true");
            return;
        }

        // Protección estricta de rutas administrativas
        if (uri != null && uri.startsWith(contextPath + "/view/admin/")) {
            boolean esAdmin = Boolean.TRUE.equals(session.getAttribute("es_admin"))
                    || Boolean.TRUE.equals(session.getAttribute("master"));
            if (!esAdmin) {
                redirectSafely(req, res, contextPath + "/view/mailbox/index.hsm?denied=true");
                return;
            }
        }

        // Evitar caché del navegador en vistas protegidas tras cerrar sesión
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        chain.doFilter(request, response);
    }

    private void redirectSafely(HttpServletRequest req, HttpServletResponse res, String targetUrl)
            throws IOException {
        boolean isAjax = "partial/ajax".equals(req.getHeader("Faces-Request"));
        if (isAjax) {
            res.setContentType("text/xml;charset=UTF-8");
            res.getWriter().printf(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\"%s\"></redirect></partial-response>",
                    targetUrl
            );
        } else {
            res.sendRedirect(targetUrl);
        }
    }
}
