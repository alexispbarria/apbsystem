package com.mycompany.apbapp.helpers;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.primefaces.PrimeFaces;

public class Util {

    private static final Logger LOG = Logger.getLogger(Util.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
    private static final Pattern RUT_CONVENTION_PATTERN = Pattern.compile("^\\d{7,8}-[0-9K]$");

    // ==========================================
    // JSF & PRIMEFACES
    // ==========================================

    public static void avisoInfo(String growlFor, String mensaje) {
        FacesContext.getCurrentInstance().addMessage(growlFor, new FacesMessage(FacesMessage.SEVERITY_INFO, mensaje, null));
    }

    public static void avisoError(String growlFor, String mensaje) {
        FacesContext.getCurrentInstance().addMessage(growlFor, new FacesMessage(FacesMessage.SEVERITY_ERROR, mensaje, null));
    }

    public static void actualizarform(String id) {
        PrimeFaces.current().ajax().update(id);
    }

    public static void resetearFormulario(String id) {
        PrimeFaces.current().resetInputs(id);
    }

    public static void ejecutarJavaScript(String comando) {
        PrimeFaces.current().executeScript(comando);
    }

    public static HttpSession getSession() {
        return (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
    }

    public static Cookie getCookie(String key) {
        Cookie cookie = (Cookie) FacesContext.getCurrentInstance().getExternalContext().getRequestCookieMap().get(key);
        if (cookie != null) {
            LOG.log(Level.FINE, "Cookie obtenida: {0}", cookie.getValue());
        }
        return cookie;
    }

    // ==========================================
    // HTTP & NETWORK
    // ==========================================

    public static String getRemoteAddr() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        return (ipAddress == null) ? request.getRemoteAddr() : ipAddress;
    }

    public static String getURLReal() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String scheme = (forwardedProto != null) ? forwardedProto : request.getScheme();

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(request.getServerName());

        if (request.getServerName().contains("localhost")) {
            if ((scheme.equals("http") && request.getServerPort() != 80)
                    || (scheme.equals("https") && request.getServerPort() != 443)) {
                baseUrl.append(":").append(request.getServerPort());
            }
        }
        return baseUrl.toString();
    }

    public static List<String> getListIps() {
        List<String> lista = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        LOG.log(Level.INFO, "Interfaz: {0} IP: {1}", new Object[]{iface.getDisplayName(), ip});
                        lista.add(ip);
                    }
                }
            }
        } catch (SocketException e) {
            LOG.log(Level.SEVERE, "Error obteniendo IPs", e);
        }
        return lista;
    }

    public static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==========================================
    // VALIDACIONES Y FORMATOS (CHILE & GENERAL)
    // ==========================================

    /**
     * Verifica si el texto cumple la convención de formato de RUT: xxxxxxxx-x
     * (7 u 8 dígitos sin puntos, guion y dígito verificador 0-9 o K).
     */
    public static boolean tieneFormatoRutConvencion(String rutValue) {
        if (rutValue == null) {
            return false;
        }
        return RUT_CONVENTION_PATTERN.matcher(rutValue.trim().toUpperCase()).matches();
    }

    /**
     * Calcula el dígito verificador chileno (Módulo 11) para la parte numérica del RUT.
     */
    public static char calcularDvRut(int numero) {
        int m = 0;
        int s = 1;
        for (int n = numero; n != 0; n /= 10) {
            s = (s + (n % 10) * (9 - (m++ % 6))) % 11;
        }
        return (char) (s != 0 ? s + 47 : 'K');
    }

    /**
     * Valida un RUT chileno bajo la convención estricta xxxxxxxx-x y su dígito verificador Módulo 11.
     */
    public static boolean validarRut(String rutValue) {
        if (rutValue == null) {
            return false;
        }
        String normalizado = rutValue.trim().toUpperCase();
        if (!RUT_CONVENTION_PATTERN.matcher(normalizado).matches()) {
            return false;
        }
        String[] partes = normalizado.split("-");
        if (partes.length != 2 || partes[1].length() != 1) {
            return false;
        }
        try {
            int rut = Integer.parseInt(partes[0]);
            if (rut <= 0) {
                return false;
            }
            char dv = partes[1].charAt(0);
            return dv == calcularDvRut(rut);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Normaliza un RUT a la convención del sistema: xxxxxxxx-x (sin puntos, DV en mayúscula).
     * Ejemplo: "12.345.678-k" o "12345678k" -> "12345678-K".
     */
    public static String formatearRUT(String rutEntrada) {
        if (rutEntrada == null || rutEntrada.isBlank()) {
            return null;
        }
        String limpio = rutEntrada.replaceAll("[.\\-\\s]", "").toUpperCase();
        if (limpio.length() < 8 || limpio.length() > 9) {
            return rutEntrada.trim().toUpperCase();
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        String dv = limpio.substring(limpio.length() - 1);
        if (!cuerpo.matches("^\\d{7,8}$") || !dv.matches("^[0-9K]$")) {
            return rutEntrada.trim().toUpperCase();
        }
        return cuerpo + "-" + dv;
    }

    public static boolean validarEmail(String email_value) {
        return email_value != null && EMAIL_PATTERN.matcher(email_value).matches();
    }

    public static String formatoCLP(Number monto) {
        if (monto == null) return "$0";
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "CL"));
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("$#,##0", simbolos).format(monto);
    }

    public static String leftPad(String originalString, int length, char padCharacter) {
        if (originalString == null) originalString = "";
        int padding = length - originalString.length();
        return padding > 0 ? String.valueOf(padCharacter).repeat(padding) + originalString : originalString;
    }

    public static String rightPad(String str, int size, char padChar) {
        if (str == null) str = "";
        int padding = size - str.length();
        return padding > 0 ? str + String.valueOf(padChar).repeat(padding) : str;
    }

    // ==========================================
    // ARCHIVOS (NIO.2 - Java 8+)
    // ==========================================


    public static void copyFileUsingStream(File source, File target) throws IOException {
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static void setPermission(File file) throws IOException {
        // Prevención para Windows (donde PosixFilePermission lanza excepción)
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.GROUP_WRITE);
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OTHERS_READ);
            perms.add(PosixFilePermission.OTHERS_WRITE);
            Files.setPosixFilePermissions(file.toPath(), perms);
        }
    }
}
