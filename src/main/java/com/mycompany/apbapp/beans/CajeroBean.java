package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.CajaTurnoController;
import com.mycompany.apbapp.controllers.ClienteController;
import com.mycompany.apbapp.controllers.ProductoController;
import com.mycompany.apbapp.controllers.VentaController;
import com.mycompany.apbapp.entities.CajaTurnos;
import com.mycompany.apbapp.entities.Clientes;
import com.mycompany.apbapp.entities.Productos;
import com.mycompany.apbapp.entities.VentaDetalles;
import com.mycompany.apbapp.entities.Ventas;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.primefaces.PrimeFaces;

/**
 * Bean de vista para la Pantalla de Cajero (POS).
 * Gestiona apertura/cierre de caja con valor inicial, búsqueda dual en Catálogo Rápido
 * (código de barras USB/cámara + búsqueda en vivo por nombre), cliente con RUT xxxxxxxx-x
 * y cobro transaccional con descuento real de stock y auditoría.
 */
@Named("cajeroBean")
@ViewScoped
public class CajeroBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CajaTurnoController cajaTurnoController;

    @Inject
    private ProductoController productoController;

    @Inject
    private ClienteController clienteController;

    @Inject
    private VentaController ventaController;

    @Inject
    private SessionBean sessionBean;

    // Estado de caja (valor de caja inicial y arqueo)
    private CajaTurnos cajaActual;
    private int montoCajaInicial = 50000;
    private String observacionApertura;
    private int montoRealCierre;
    private String observacionCierre;

    // Catálogo rápido: 2 modos de búsqueda
    private String codigoBarraBusqueda;
    private String terminoNombreBusqueda;
    private List<Productos> resultadosBusqueda = new ArrayList<>();

    // Carrito / Ticket
    private List<VentaDetalles> carrito = new ArrayList<>();
    private String metodoPago = "EFECTIVO";
    private int descuento;
    private int montoPagado;
    private String observacionVenta;

    // Cliente (convención RUT xxxxxxxx-x)
    private String rutClienteBusqueda;
    private Clientes clienteSeleccionado;
    private boolean mostrarAltaRapidaCliente;
    private String nuevoClienteNombre;
    private String nuevoClienteEmail;
    private String nuevoClienteTelefono;

    // Resumen última venta registrada
    private Ventas ultimaVentaRegistrada;

    @PostConstruct
    public void init() {
        cargarCajaAbierta();
        buscarProductosPorNombre();
    }

    public void cargarCajaAbierta() {
        if (sessionBean.getUsuarioActual() != null && sessionBean.getUsuarioActual().getId() != null) {
            this.cajaActual = cajaTurnoController.findCajaAbiertaPorUsuario(sessionBean.getUsuarioActual().getId());
            if (this.cajaActual != null) {
                this.montoRealCierre = this.cajaActual.getMontoEsperadoCierre();
            }
        }
    }

    public void abrirTurnoCaja() {
        try {
            this.cajaActual = cajaTurnoController.abrirCaja(
                    sessionBean.getUsuarioActual(),
                    this.montoCajaInicial,
                    this.observacionApertura,
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            this.observacionApertura = null;
            this.montoRealCierre = this.cajaActual.getMontoEsperadoCierre();
            Util.avisoInfo(null, "Turno de caja #" + this.cajaActual.getId()
                    + " abierto con caja inicial de " + Util.formatoCLP(this.cajaActual.getMontoInicial()));
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void cerrarTurnoCaja() {
        if (this.cajaActual == null) {
            Util.avisoError(null, "No existe un turno de caja abierto.");
            return;
        }
        try {
            CajaTurnos cerrada = cajaTurnoController.cerrarCaja(
                    this.cajaActual.getId(),
                    this.montoRealCierre,
                    this.observacionCierre,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            this.cajaActual = null;
            this.carrito.clear();
            this.observacionCierre = null;
            int diff = cerrada.getDiferenciaCierre() != null ? cerrada.getDiferenciaCierre() : 0;
            Util.avisoInfo(null, "Caja #" + cerrada.getId() + " cerrada. Diferencia de arqueo: " + Util.formatoCLP(diff));
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    /**
     * Búsqueda 1 del Catálogo Rápido: Código de barras (pistola láser USB o cámara html5-qrcode).
     */
    public void buscarYAgregarPorCodigoBarra() {
        String codigo = this.codigoBarraBusqueda;

        // Soporte para parámetro directo enviado desde <p:remoteCommand> del escáner de cámara
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null && fc.getExternalContext() != null) {
            Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
            String scanned = params.get("scannedCode");
            if (scanned != null && !scanned.isBlank()) {
                codigo = scanned;
            }
        }

        if (codigo == null || codigo.isBlank()) {
            Util.avisoError(null, "Escanea o ingresa un código de barras.");
            PrimeFaces.current().ajax().addCallbackParam("scanFound", false);
            return;
        }

        Productos prod = productoController.findByCodigoBarra(codigo.trim());
        this.codigoBarraBusqueda = null;

        if (prod == null) {
            Util.avisoError(null, "No se encontró ningún producto activo con el código de barras '" + codigo.trim() + "'.");
            PrimeFaces.current().ajax().addCallbackParam("scanFound", false);
            return;
        }

        boolean ok = agregarProducto(prod);
        PrimeFaces.current().ajax().addCallbackParam("scanFound", ok);
    }

    /**
     * Búsqueda 2 del Catálogo Rápido: Búsqueda en vivo por nombre / SKU / categoría.
     */
    public void buscarProductosPorNombre() {
        this.resultadosBusqueda = productoController.buscarPorNombreOCodigo(this.terminoNombreBusqueda, 24);
    }

    public void limpiarBusquedaNombre() {
        this.terminoNombreBusqueda = null;
        buscarProductosPorNombre();
    }

    public boolean agregarProducto(Productos prod) {
        if (prod == null) {
            return false;
        }
        if (prod.getStockActual() <= 0) {
            Util.avisoError(null, "Sin stock disponible para '" + prod.getNombre() + "'.");
            return false;
        }

        for (VentaDetalles item : this.carrito) {
            if (item.getProducto() != null && item.getProducto().getId().equals(prod.getId())) {
                int nuevaCantidad = item.getCantidad() + 1;
                if (nuevaCantidad > prod.getStockActual()) {
                    Util.avisoError(null, "Stock máximo alcanzado para '" + prod.getNombre()
                            + "' (disponible: " + prod.getStockActual() + ").");
                    return false;
                }
                item.setCantidad(nuevaCantidad);
                item.setSubtotal(nuevaCantidad * item.getPrecioUnitario());
                sincronizarMontoPagadoSiNoEfectivo();
                Util.avisoInfo(null, "+1 " + prod.getNombre() + " (en ticket: " + nuevaCantidad + ")");
                return true;
            }
        }

        VentaDetalles nuevo = new VentaDetalles();
        nuevo.setProducto(prod);
        nuevo.setProductoNombre(prod.getNombre());
        nuevo.setCodigoBarra(prod.getCodigoBarra());
        nuevo.setPrecioUnitario(prod.getPrecioVenta());
        nuevo.setCantidad(1);
        nuevo.setSubtotal(prod.getPrecioVenta());
        this.carrito.add(nuevo);
        sincronizarMontoPagadoSiNoEfectivo();
        Util.avisoInfo(null, "Agregado: " + prod.getNombre() + " (" + Util.formatoCLP(prod.getPrecioVenta()) + ")");
        return true;
    }

    public void incrementarCantidad(VentaDetalles item) {
        if (item == null || item.getProducto() == null) {
            return;
        }
        int stockDisp = item.getProducto().getStockActual();
        if (item.getCantidad() + 1 > stockDisp) {
            Util.avisoError(null, "Stock insuficiente para '" + item.getProductoNombre() + "' (máximo: " + stockDisp + ").");
            return;
        }
        item.setCantidad(item.getCantidad() + 1);
        item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
        sincronizarMontoPagadoSiNoEfectivo();
    }

    public void decrementarCantidad(VentaDetalles item) {
        if (item == null) {
            return;
        }
        if (item.getCantidad() <= 1) {
            this.carrito.remove(item);
        } else {
            item.setCantidad(item.getCantidad() - 1);
            item.setSubtotal(item.getCantidad() * item.getPrecioUnitario());
        }
        sincronizarMontoPagadoSiNoEfectivo();
    }

    public void eliminarItem(VentaDetalles item) {
        this.carrito.remove(item);
        sincronizarMontoPagadoSiNoEfectivo();
    }

    public void vaciarCarrito() {
        this.carrito.clear();
        this.descuento = 0;
        this.montoPagado = 0;
        this.observacionVenta = null;
    }

    public void seleccionarMetodoPago(String metodo) {
        this.metodoPago = metodo;
        sincronizarMontoPagadoSiNoEfectivo();
    }

    public void aplicarMontoExacto() {
        this.montoPagado = getTotal();
    }

    private void sincronizarMontoPagadoSiNoEfectivo() {
        if (!"EFECTIVO".equalsIgnoreCase(this.metodoPago)) {
            this.montoPagado = getTotal();
        }
    }

    // ---- Gestión rápida de cliente (RUT xxxxxxxx-x) ----

    public void buscarClientePorRut() {
        this.mostrarAltaRapidaCliente = false;
        if (this.rutClienteBusqueda == null || this.rutClienteBusqueda.isBlank()) {
            this.clienteSeleccionado = null;
            Util.avisoInfo(null, "Venta configurada como Consumidor Final.");
            return;
        }
        Clientes cli = clienteController.findByRut(this.rutClienteBusqueda);
        if (cli != null) {
            this.clienteSeleccionado = cli;
            Util.avisoInfo(null, "Cliente asociado: " + cli.getNombreCompleto() + " (" + cli.getRut() + ")");
        } else {
            this.clienteSeleccionado = null;
            this.mostrarAltaRapidaCliente = true;
            Util.avisoInfo(null, "RUT válido no registrado aún. Puedes crear el cliente aquí mismo.");
        }
    }

    public void crearYAsociarClienteRapido() {
        try {
            Clientes nuevo = new Clientes();
            nuevo.setRut(this.rutClienteBusqueda);
            nuevo.setNombreCompleto(this.nuevoClienteNombre);
            nuevo.setEmail(this.nuevoClienteEmail);
            nuevo.setTelefono(this.nuevoClienteTelefono);

            this.clienteSeleccionado = clienteController.guardarCliente(
                    nuevo,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            this.mostrarAltaRapidaCliente = false;
            this.nuevoClienteNombre = null;
            this.nuevoClienteEmail = null;
            this.nuevoClienteTelefono = null;
            Util.avisoInfo(null, "Cliente registrado y asociado: " + this.clienteSeleccionado.getNombreCompleto());
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void quitarCliente() {
        this.clienteSeleccionado = null;
        this.rutClienteBusqueda = null;
        this.mostrarAltaRapidaCliente = false;
    }

    // ---- Registro transaccional de venta ----

    public void finalizarVenta() {
        if (this.cajaActual == null || !this.cajaActual.isAbierta()) {
            Util.avisoError(null, "Debes abrir un turno de caja antes de cobrar.");
            return;
        }
        if (this.carrito.isEmpty()) {
            Util.avisoError(null, "Agrega al menos un producto al ticket.");
            return;
        }

        try {
            int pagado = "EFECTIVO".equalsIgnoreCase(this.metodoPago) ? this.montoPagado : getTotal();
            if ("EFECTIVO".equalsIgnoreCase(this.metodoPago) && pagado == 0) {
                pagado = getTotal();
            }

            Ventas registrada = ventaController.registrarVenta(
                    this.cajaActual.getId(),
                    sessionBean.getUsuarioActual(),
                    this.clienteSeleccionado != null ? this.clienteSeleccionado.getId() : null,
                    this.carrito,
                    this.metodoPago,
                    this.descuento,
                    pagado,
                    this.observacionVenta,
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );

            this.ultimaVentaRegistrada = registrada;
            vaciarCarrito();
            quitarCliente();
            cargarCajaAbierta();
            buscarProductosPorNombre();

            String msgVuelto = registrada.getVuelto() > 0
                    ? " · Vuelto: " + Util.formatoCLP(registrada.getVuelto())
                    : "";
            Util.avisoInfo(null, "Venta " + registrada.getFolio() + " registrada por "
                    + Util.formatoCLP(registrada.getTotal()) + msgVuelto);
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    // ---- Cálculos y formatos ----

    public int getSubtotal() {
        int sum = 0;
        for (VentaDetalles d : this.carrito) {
            sum += d.getSubtotal();
        }
        return sum;
    }

    public int getTotal() {
        return Math.max(0, getSubtotal() - Math.max(0, this.descuento));
    }

    public int getVuelto() {
        if (!"EFECTIVO".equalsIgnoreCase(this.metodoPago)) {
            return 0;
        }
        return Math.max(0, this.montoPagado - getTotal());
    }

    public int getTotalUnidades() {
        int sum = 0;
        for (VentaDetalles d : this.carrito) {
            sum += d.getCantidad();
        }
        return sum;
    }

    public boolean isCajaAbierta() {
        return this.cajaActual != null && this.cajaActual.isAbierta();
    }

    public String formatoCLP(Number valor) {
        return Util.formatoCLP(valor);
    }

    // ---- Getters & Setters ----

    public CajaTurnos getCajaActual() {
        return cajaActual;
    }

    public int getMontoCajaInicial() {
        return montoCajaInicial;
    }

    public void setMontoCajaInicial(int montoCajaInicial) {
        this.montoCajaInicial = montoCajaInicial;
    }

    public String getObservacionApertura() {
        return observacionApertura;
    }

    public void setObservacionApertura(String observacionApertura) {
        this.observacionApertura = observacionApertura;
    }

    public int getMontoRealCierre() {
        return montoRealCierre;
    }

    public void setMontoRealCierre(int montoRealCierre) {
        this.montoRealCierre = montoRealCierre;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }

    public void setObservacionCierre(String observacionCierre) {
        this.observacionCierre = observacionCierre;
    }

    public String getCodigoBarraBusqueda() {
        return codigoBarraBusqueda;
    }

    public void setCodigoBarraBusqueda(String codigoBarraBusqueda) {
        this.codigoBarraBusqueda = codigoBarraBusqueda;
    }

    public String getTerminoNombreBusqueda() {
        return terminoNombreBusqueda;
    }

    public void setTerminoNombreBusqueda(String terminoNombreBusqueda) {
        this.terminoNombreBusqueda = terminoNombreBusqueda;
    }

    public List<Productos> getResultadosBusqueda() {
        return resultadosBusqueda;
    }

    public List<VentaDetalles> getCarrito() {
        return carrito;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public int getDescuento() {
        return descuento;
    }

    public void setDescuento(int descuento) {
        this.descuento = descuento;
    }

    public int getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(int montoPagado) {
        this.montoPagado = montoPagado;
    }

    public String getObservacionVenta() {
        return observacionVenta;
    }

    public void setObservacionVenta(String observacionVenta) {
        this.observacionVenta = observacionVenta;
    }

    public String getRutClienteBusqueda() {
        return rutClienteBusqueda;
    }

    public void setRutClienteBusqueda(String rutClienteBusqueda) {
        this.rutClienteBusqueda = rutClienteBusqueda;
    }

    public Clientes getClienteSeleccionado() {
        return clienteSeleccionado;
    }

    public boolean isMostrarAltaRapidaCliente() {
        return mostrarAltaRapidaCliente;
    }

    public String getNuevoClienteNombre() {
        return nuevoClienteNombre;
    }

    public void setNuevoClienteNombre(String nuevoClienteNombre) {
        this.nuevoClienteNombre = nuevoClienteNombre;
    }

    public String getNuevoClienteEmail() {
        return nuevoClienteEmail;
    }

    public void setNuevoClienteEmail(String nuevoClienteEmail) {
        this.nuevoClienteEmail = nuevoClienteEmail;
    }

    public String getNuevoClienteTelefono() {
        return nuevoClienteTelefono;
    }

    public void setNuevoClienteTelefono(String nuevoClienteTelefono) {
        this.nuevoClienteTelefono = nuevoClienteTelefono;
    }

    public Ventas getUltimaVentaRegistrada() {
        return ultimaVentaRegistrada;
    }
}
