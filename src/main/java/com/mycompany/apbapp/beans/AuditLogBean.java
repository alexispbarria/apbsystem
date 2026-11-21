package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.AuditLogController;
import com.mycompany.apbapp.entities.AuditLogs;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Bean de vista exclusivo para el panel de Logs de Auditoría (/view/admin/logs.hsm).
 * Protegido a nivel de ruta por AuthFilter (/view/admin/*) solo para Administradores.
 */
@Named("auditLogBean")
@ViewScoped
public class AuditLogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AuditLogController auditLogController;

    private List<AuditLogs> logs = new ArrayList<>();
    private List<String> modulosDisponibles = new ArrayList<>();

    private String filtroModulo = "TODOS";
    private String filtroUsername;
    private String filtroTexto;
    private Date fechaDesde;
    private Date fechaHasta;
    private int limite = 150;

    private AuditLogs logSeleccionado;

    @PostConstruct
    public void init() {
        this.modulosDisponibles = auditLogController.listarModulosDistintos();
        buscar();
    }

    public void buscar() {
        Date hastaAjustada = this.fechaHasta;
        if (hastaAjustada != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(hastaAjustada);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            hastaAjustada = cal.getTime();
        }
        this.logs = auditLogController.buscarLogs(
                this.filtroModulo,
                this.filtroUsername,
                this.filtroTexto,
                this.fechaDesde,
                hastaAjustada,
                this.limite
        );
    }

    public void limpiarFiltros() {
        this.filtroModulo = "TODOS";
        this.filtroUsername = null;
        this.filtroTexto = null;
        this.fechaDesde = null;
        this.fechaHasta = null;
        this.modulosDisponibles = auditLogController.listarModulosDistintos();
        buscar();
    }

    public void seleccionarLog(AuditLogs log) {
        this.logSeleccionado = log;
    }

    public String getModuloBadgeClass(String modulo) {
        if (modulo == null) {
            return "tag-soon";
        }
        switch (modulo.toUpperCase()) {
            case "VENTAS":
            case "CAJA":
                return "tag-pos";
            case "AUTH":
            case "USUARIOS":
                return "tag-admin";
            case "PRODUCTOS":
            case "INVENTARIO":
            case "CLIENTES":
                return "tag-live";
            default:
                return "tag-soon";
        }
    }

    public List<AuditLogs> getLogs() {
        return logs;
    }

    public List<String> getModulosDisponibles() {
        return modulosDisponibles;
    }

    public String getFiltroModulo() {
        return filtroModulo;
    }

    public void setFiltroModulo(String filtroModulo) {
        this.filtroModulo = filtroModulo;
    }

    public String getFiltroUsername() {
        return filtroUsername;
    }

    public void setFiltroUsername(String filtroUsername) {
        this.filtroUsername = filtroUsername;
    }

    public String getFiltroTexto() {
        return filtroTexto;
    }

    public void setFiltroTexto(String filtroTexto) {
        this.filtroTexto = filtroTexto;
    }

    public Date getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(Date fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public Date getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(Date fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public int getLimite() {
        return limite;
    }

    public void setLimite(int limite) {
        this.limite = limite;
    }

    public AuditLogs getLogSeleccionado() {
        return logSeleccionado;
    }
}
