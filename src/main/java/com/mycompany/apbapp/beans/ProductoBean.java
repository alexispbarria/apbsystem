package com.mycompany.apbapp.beans;

import com.mycompany.apbapp.controllers.CategoriaController;
import com.mycompany.apbapp.controllers.MovimientoStockController;
import com.mycompany.apbapp.controllers.ProductoController;
import com.mycompany.apbapp.entities.Categorias;
import com.mycompany.apbapp.entities.MovimientosStock;
import com.mycompany.apbapp.entities.Productos;
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
 * Bean de vista para Catálogo de Productos, Inventario y Kardex (MovimientosStock).
 * Todos los usuarios pueden consultar catálogo y precios; solo ADMIN puede crear/editar
 * productos, categorías y aplicar ajustes manuales de stock.
 */
@Named("productoBean")
@ViewScoped
public class ProductoBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ProductoController productoController;

    @Inject
    private CategoriaController categoriaController;

    @Inject
    private MovimientoStockController movimientoStockController;

    @Inject
    private SessionBean sessionBean;

    private List<Productos> productos = new ArrayList<>();
    private List<Categorias> categorias = new ArrayList<>();
    private List<MovimientosStock> movimientosRecientes = new ArrayList<>();

    private String filtroTexto;
    private boolean soloStockBajo;

    // Formulario Producto
    private Productos productoForm = new Productos();
    private Integer categoriaIdSeleccionada;

    // Formulario Categoría
    private Categorias categoriaForm = new Categorias();

    // Formulario Ajuste de Stock
    private Productos productoAjuste;
    private String tipoMovimientoAjuste = "ENTRADA";
    private int cantidadAjuste = 1;
    private String observacionAjuste;

    @PostConstruct
    public void init() {
        cargarDatos();
    }

    public void cargarDatos() {
        this.categorias = categoriaController.findActivas();
        buscarProductos();
        if (sessionBean.isAdminView()) {
            this.movimientosRecientes = movimientoStockController.findRecientes(40);
        }
    }

    public void buscarProductos() {
        List<Productos> base = productoController.buscarPorNombreOCodigo(this.filtroTexto, 200);
        if (this.soloStockBajo) {
            List<Productos> filtrados = new ArrayList<>();
            for (Productos p : base) {
                if (p.isStockBajo()) {
                    filtrados.add(p);
                }
            }
            this.productos = filtrados;
        } else {
            this.productos = base;
        }
    }

    public void abrirNuevoProducto() {
        this.productoForm = new Productos();
        this.productoForm.setEnabled(true);
        this.productoForm.setStockMinimo(5);
        this.productoForm.setUnidadMedida("UN");
        this.categoriaIdSeleccionada = !this.categorias.isEmpty() ? this.categorias.get(0).getId() : null;
    }

    public void abrirEditarProducto(Productos prod) {
        if (prod == null) {
            return;
        }
        this.productoForm = new Productos();
        this.productoForm.setId(prod.getId());
        this.productoForm.setCodigoBarra(prod.getCodigoBarra());
        this.productoForm.setSku(prod.getSku());
        this.productoForm.setNombre(prod.getNombre());
        this.productoForm.setDescripcion(prod.getDescripcion());
        this.productoForm.setPrecioCosto(prod.getPrecioCosto());
        this.productoForm.setPrecioVenta(prod.getPrecioVenta());
        this.productoForm.setStockActual(prod.getStockActual());
        this.productoForm.setStockMinimo(prod.getStockMinimo());
        this.productoForm.setUnidadMedida(prod.getUnidadMedida());
        this.productoForm.setEnabled(prod.getEnabled());
        this.categoriaIdSeleccionada = prod.getCategoria() != null ? prod.getCategoria().getId() : null;
    }

    public void guardarProducto() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede crear o modificar productos.");
            return;
        }
        try {
            boolean eraNuevo = (this.productoForm.getId() == null);
            productoController.guardarProducto(
                    this.productoForm,
                    this.categoriaIdSeleccionada,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            cargarDatos();
            Util.avisoInfo(null, eraNuevo ? "Producto creado correctamente." : "Producto actualizado correctamente.");
            PrimeFaces.current().executeScript("PF('dlgProducto').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void abrirNuevaCategoria() {
        this.categoriaForm = new Categorias();
        this.categoriaForm.setEnabled(true);
    }

    public void guardarCategoria() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede gestionar categorías.");
            return;
        }
        try {
            categoriaController.guardarCategoria(
                    this.categoriaForm,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            cargarDatos();
            Util.avisoInfo(null, "Categoría guardada correctamente.");
            PrimeFaces.current().executeScript("PF('dlgCategoria').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public void abrirAjusteStock(Productos prod) {
        this.productoAjuste = prod;
        this.tipoMovimientoAjuste = "ENTRADA";
        this.cantidadAjuste = 1;
        this.observacionAjuste = null;
    }

    public void aplicarAjusteStock() {
        if (!sessionBean.isRealAdmin()) {
            Util.avisoError(null, "Solo el Administrador puede ajustar inventario.");
            return;
        }
        if (this.productoAjuste == null) {
            Util.avisoError(null, "Selecciona un producto.");
            return;
        }
        try {
            Productos actualizado = productoController.ajustarStock(
                    this.productoAjuste.getId(),
                    this.tipoMovimientoAjuste,
                    this.cantidadAjuste,
                    this.observacionAjuste,
                    sessionBean.getUsuarioActual(),
                    sessionBean.getRolCodigo(),
                    Util.getRemoteAddr()
            );
            cargarDatos();
            Util.avisoInfo(null, "Stock actualizado para '" + actualizado.getNombre()
                    + "': ahora tiene " + actualizado.getStockActual() + " u.");
            PrimeFaces.current().executeScript("PF('dlgAjusteStock').hide();");
        } catch (NegocioException ex) {
            Util.avisoError(null, ex.getMessage());
        }
    }

    public long getTotalStockBajo() {
        return productoController.contarStockBajo();
    }

    public String formatoCLP(Number valor) {
        return Util.formatoCLP(valor);
    }

    // ---- Getters & Setters ----

    public List<Productos> getProductos() {
        return productos;
    }

    public List<Categorias> getCategorias() {
        return categorias;
    }

    public List<MovimientosStock> getMovimientosRecientes() {
        return movimientosRecientes;
    }

    public String getFiltroTexto() {
        return filtroTexto;
    }

    public void setFiltroTexto(String filtroTexto) {
        this.filtroTexto = filtroTexto;
    }

    public boolean isSoloStockBajo() {
        return soloStockBajo;
    }

    public void setSoloStockBajo(boolean soloStockBajo) {
        this.soloStockBajo = soloStockBajo;
    }

    public Productos getProductoForm() {
        return productoForm;
    }

    public void setProductoForm(Productos productoForm) {
        this.productoForm = productoForm;
    }

    public Integer getCategoriaIdSeleccionada() {
        return categoriaIdSeleccionada;
    }

    public void setCategoriaIdSeleccionada(Integer categoriaIdSeleccionada) {
        this.categoriaIdSeleccionada = categoriaIdSeleccionada;
    }

    public Categorias getCategoriaForm() {
        return categoriaForm;
    }

    public void setCategoriaForm(Categorias categoriaForm) {
        this.categoriaForm = categoriaForm;
    }

    public Productos getProductoAjuste() {
        return productoAjuste;
    }

    public String getTipoMovimientoAjuste() {
        return tipoMovimientoAjuste;
    }

    public void setTipoMovimientoAjuste(String tipoMovimientoAjuste) {
        this.tipoMovimientoAjuste = tipoMovimientoAjuste;
    }

    public int getCantidadAjuste() {
        return cantidadAjuste;
    }

    public void setCantidadAjuste(int cantidadAjuste) {
        this.cantidadAjuste = cantidadAjuste;
    }

    public String getObservacionAjuste() {
        return observacionAjuste;
    }

    public void setObservacionAjuste(String observacionAjuste) {
        this.observacionAjuste = observacionAjuste;
    }
}
