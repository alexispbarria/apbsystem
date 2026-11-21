package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.VentaController;
import com.mycompany.apbapp.entities.Ventas;
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
 * Bean de vista para el Historial de Ventas, consulta de detalles y anulación (solo ADMIN).
 */
@Named("ventaHistorialBean")
@ViewScoped
public class VentaHistorialBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private VentaController ventaController;

    @Inject
    private SessionBean sessionBean;

    private List<Ventas> ventas = new ArrayList<>();
    private String filtroFolio;
    private String filtroMetodoPago = "TODOS";
    private String filtroEstado = "TODOS";

    private Ventas ventaDetalle;
    private Ventas ventaPorAnular;
    private String motivoAnulacion;

    @PostConstruct
    public void init() {
        buscar();
    }

    public void buscar() {
        Integer uidFiltro = null;
        if (sessionBean.isTrabajadorView() && sessionBean.getUsuarioActual() != null) {
            uidFiltro = sessionBean.getUsuarioActual().getId();
        }
        this.ventas = ventaController.buscarVentas(
                this.filtroFolio,
                this.filtroMetodoPago,
                this.filtroEstado,
                uidFiltro,
                150
        );
    }

    public void limpiarFiltros() {
        this.filtroFolio = null;
        this.filtroMetodoPago = "TODOS";
        this.filtroEstado = "TODOS";
        buscar();
    }

    public void verDetalle(Integer ventaId) {
        this.ventaDetalle = ventaController.findConDetallesById(ventaId);
    }

    public void prepararAnulacion(Ventas venta) {
        this.ventaPorAnular = venta;
        this.motivoAnulacion = null;
    }

    public void confirmarAnulacion() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede anular ventas.");
            return;
        }
        if (this.ventaPorAnular == null) {
            return;
        }
        try {
            Ventas anulada = ventaController.anularVenta(
                    this.ventaPorAnular.getId(),
                    sessionBean.getUsuarioActual(),
                    this.motivoAnulacion,
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            buscar();
            Util.avisoInfo(null, "Venta " + anulada.getFolio() + " anulada. El stock fue restaurado.");
            PrimeFaces.current().executeScript("PF('dlgAnularVenta').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public long getMontoCompletadasListado() {
        long total = 0;
        for (Ventas v : this.ventas) {
            if ("COMPLETADA".equalsIgnoreCase(v.getEstado())) {
                total += v.getTotal();
            }
        }
        return total;
    }

    public int getCantidadCompletadasListado() {
        int count = 0;
        for (Ventas v : this.ventas) {
            if ("COMPLETADA".equalsIgnoreCase(v.getEstado())) {
                count++;
            }
        }
        return count;
    }

    public String formatoCLP(Number valor) {
        return Util.formatoCLP(valor);
    }

    public List<Ventas> getVentas() {
        return ventas;
    }

    public String getFiltroFolio() {
        return filtroFolio;
    }

    public void setFiltroFolio(String filtroFolio) {
        this.filtroFolio = filtroFolio;
    }

    public String getFiltroMetodoPago() {
        return filtroMetodoPago;
    }

    public void setFiltroMetodoPago(String filtroMetodoPago) {
        this.filtroMetodoPago = filtroMetodoPago;
    }

    public String getFiltroEstado() {
        return filtroEstado;
    }

    public void setFiltroEstado(String filtroEstado) {
        this.filtroEstado = filtroEstado;
    }

    public Ventas getVentaDetalle() {
        return ventaDetalle;
    }

    public Ventas getVentaPorAnular() {
        return ventaPorAnular;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}
