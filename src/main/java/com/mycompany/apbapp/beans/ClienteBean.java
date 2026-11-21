package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.ClienteController;
import com.mycompany.apbapp.controllers.VentaController;
import com.mycompany.apbapp.entities.Clientes;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.primefaces.PrimeFaces;

/**
 * Bean de vista para gestión de Clientes (RUT xxxxxxxx-x) y métricas de compra por cliente.
 */
@Named("clienteBean")
@ViewScoped
public class ClienteBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClienteController clienteController;

    @Inject
    private VentaController ventaController;

    @Inject
    private SessionBean sessionBean;

    private List<Clientes> clientes = new ArrayList<>();
    private Map<Integer, Long> comprasCount = new HashMap<>();
    private Map<Integer, Long> comprasTotal = new HashMap<>();

    private String filtroTexto;
    private Clientes clienteForm = new Clientes();

    @PostConstruct
    public void init() {
        buscar();
    }

    public void buscar() {
        this.clientes = clienteController.buscar(this.filtroTexto, 200);
        this.comprasCount.clear();
        this.comprasTotal.clear();
        for (Clientes c : this.clientes) {
            if (c.getId() != null) {
                this.comprasCount.put(c.getId(), ventaController.contarVentasPorCliente(c.getId()));
                this.comprasTotal.put(c.getId(), ventaController.sumarComprasPorCliente(c.getId()));
            }
        }
    }

    public void abrirNuevoCliente() {
        this.clienteForm = new Clientes();
        this.clienteForm.setEnabled(true);
    }

    public void abrirEditarCliente(Clientes cli) {
        if (cli == null) {
            return;
        }
        this.clienteForm = new Clientes();
        this.clienteForm.setId(cli.getId());
        this.clienteForm.setRut(cli.getRut());
        this.clienteForm.setNombreCompleto(cli.getNombreCompleto());
        this.clienteForm.setEmail(cli.getEmail());
        this.clienteForm.setTelefono(cli.getTelefono());
        this.clienteForm.setDireccion(cli.getDireccion());
        this.clienteForm.setObservaciones(cli.getObservaciones());
        this.clienteForm.setEnabled(cli.getEnabled());
    }

    public void guardarCliente() {
        try {
            boolean eraNuevo = (this.clienteForm.getId() == null);
            clienteController.guardarCliente(
                    this.clienteForm,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            buscar();
            Util.avisoInfo(null, eraNuevo ? "Cliente registrado correctamente." : "Cliente actualizado correctamente.");
            PrimeFaces.current().executeScript("PF('dlgCliente').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public long getCantidadCompras(Integer clienteId) {
        return this.comprasCount.getOrDefault(clienteId, 0L);
    }

    public String getTotalComprasFormateado(Integer clienteId) {
        return Util.formatoCLP(this.comprasTotal.getOrDefault(clienteId, 0L));
    }

    public List<Clientes> getClientes() {
        return clientes;
    }

    public String getFiltroTexto() {
        return filtroTexto;
    }

    public void setFiltroTexto(String filtroTexto) {
        this.filtroTexto = filtroTexto;
    }

    public Clientes getClienteForm() {
        return clienteForm;
    }

    public void setClienteForm(Clientes clienteForm) {
        this.clienteForm = clienteForm;
    }
}
