package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.AuditLogController;
import com.mycompany.apbapp.controllers.CajaTurnoController;
import com.mycompany.apbapp.controllers.ClienteController;
import com.mycompany.apbapp.controllers.ProductoController;
import com.mycompany.apbapp.controllers.UsersController;
import com.mycompany.apbapp.controllers.VentaController;
import com.mycompany.apbapp.entities.AuditLogs;
import com.mycompany.apbapp.entities.CajaTurnos;
import com.mycompany.apbapp.entities.Productos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.helpers.Util;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bean de vista para el Dashboard principal (/view/mailbox/index.hsm).
 * Consulta métricas reales de ventas, caja, productos, clientes, usuarios y auditoría.
 */
@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UsersController usersController;

    @Inject
    private VentaController ventaController;

    @Inject
    private ProductoController productoController;

    @Inject
    private ClienteController clienteController;

    @Inject
    private CajaTurnoController cajaTurnoController;

    @Inject
    private AuditLogController auditLogController;

    @Inject
    private SessionBean sessionBean;

    private int totalUsuarios;
    private long ventasHoyMonto;
    private long transaccionesHoy;
    private long totalProductosActivos;
    private long totalStockBajo;
    private long totalClientes;

    private CajaTurnos cajaActiva;
    private List<Productos> productosStockBajo = new ArrayList<>();
    private List<AuditLogs> ultimosLogs = new ArrayList<>();

    private String saludoDinamico;
    private String fechaActualFormateada;

    @PostConstruct
    public void init() {
        cargarSaludoYFecha();
        cargarMetricasReales();
        verificarParametroAccesoDenegado();
    }

    private void cargarSaludoYFecha() {
        int hora = LocalTime.now().getHour();
        if (hora < 12) {
            this.saludoDinamico = "Buenos días";
        } else if (hora < 20) {
            this.saludoDinamico = "Buenas tardes";
        } else {
            this.saludoDinamico = "Buenas noches";
        }

        Locale localeCL = new Locale("es", "CL");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, yyyy", localeCL);
        String fecha = LocalDate.now().format(fmt);
        this.fechaActualFormateada = fecha.isEmpty()
                ? fecha
                : Character.toUpperCase(fecha.charAt(0)) + fecha.substring(1);
    }

    private void cargarMetricasReales() {
        try {
            List<Users> lista = usersController.findAll();
            this.totalUsuarios = lista != null ? lista.size() : 0;
        } catch (Exception ex) {
            this.totalUsuarios = 0;
        }

        try {
            this.ventasHoyMonto = ventaController.sumarVentasHoy();
            this.transaccionesHoy = ventaController.contarVentasHoy();
        } catch (Exception ex) {
            this.ventasHoyMonto = 0L;
            this.transaccionesHoy = 0L;
        }

        try {
            this.totalProductosActivos = productoController.contarActivos();
            this.totalStockBajo = productoController.contarStockBajo();
            this.productosStockBajo = productoController.findStockBajo(5);
        } catch (Exception ex) {
            this.totalProductosActivos = 0L;
            this.totalStockBajo = 0L;
            this.productosStockBajo = new ArrayList<>();
        }

        try {
            this.totalClientes = clienteController.contarActivos();
        } catch (Exception ex) {
            this.totalClientes = 0L;
        }

        try {
            if (sessionBean.getUsuarioActual() != null && sessionBean.getUsuarioActual().getId() != null) {
                this.cajaActiva = cajaTurnoController.findCajaAbiertaPorUsuario(sessionBean.getUsuarioActual().getId());
            }
            if (this.cajaActiva == null && sessionBean.isAdminView()) {
                this.cajaActiva = cajaTurnoController.findUltimaCajaAbierta();
            }
        } catch (Exception ex) {
            this.cajaActiva = null;
        }

        try {
            if (sessionBean.isAdminView()) {
                this.ultimosLogs = auditLogController.buscarLogs(null, null, null, null, null, 6);
            }
        } catch (Exception ex) {
            this.ultimosLogs = new ArrayList<>();
        }
    }

    private void verificarParametroAccesoDenegado() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getExternalContext() == null) {
            return;
        }
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        if (Boolean.parseBoolean(params.get("denied"))) {
            Util.avisoError(null, "Acceso restringido: esta sección es exclusiva para el Administrador.");
        }
    }

    public String getVentasHoyFormateado() {
        return Util.formatoCLP(this.ventasHoyMonto);
    }

    public String getEstadoCajaTexto() {
        if (this.cajaActiva != null && this.cajaActiva.isAbierta()) {
            return "Caja #" + this.cajaActiva.getId() + " Abierta";
        }
        return "Sin caja abierta";
    }

    public String getCajaInicialTexto() {
        if (this.cajaActiva != null && this.cajaActiva.isAbierta()) {
            return "Inicial: " + Util.formatoCLP(this.cajaActiva.getMontoInicial())
                    + " · Esperado efectivo: " + Util.formatoCLP(this.cajaActiva.getMontoEsperadoCierre());
        }
        return "Abre tu caja inicial desde Pantalla de Cajero";
    }

    public String formatoCLP(Number valor) {
        return Util.formatoCLP(valor);
    }

    public int getTotalUsuarios() {
        return totalUsuarios;
    }

    public long getVentasHoyMonto() {
        return ventasHoyMonto;
    }

    public long getTransaccionesHoy() {
        return transaccionesHoy;
    }

    public long getTotalProductosActivos() {
        return totalProductosActivos;
    }

    public long getTotalStockBajo() {
        return totalStockBajo;
    }

    public long getTotalClientes() {
        return totalClientes;
    }

    public CajaTurnos getCajaActiva() {
        return cajaActiva;
    }

    public List<Productos> getProductosStockBajo() {
        return productosStockBajo;
    }

    public List<AuditLogs> getUltimosLogs() {
        return ultimosLogs;
    }

    public String getSaludoDinamico() {
        return saludoDinamico;
    }

    public String getFechaActualFormateada() {
        return fechaActualFormateada;
    }

    public SessionBean getSessionBean() {
        return sessionBean;
    }
}
