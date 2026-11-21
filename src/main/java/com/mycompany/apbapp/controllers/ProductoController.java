package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Categorias;
import com.mycompany.apbapp.entities.MovimientosStock;
import com.mycompany.apbapp.entities.Productos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.exceptions.NegocioException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class ProductoController extends AbstractDao<Productos> {

    @Inject
    private AuditLogController auditLogController;

    public ProductoController() {
        super(Productos.class);
    }

    @Override
    public List<Productos> findAll() {
        List<Productos> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT p FROM Productos p LEFT JOIN FETCH p.categoria ORDER BY p.nombre ASC",
                    Productos.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll", ex);
        }
        return lista;
    }

    public List<Productos> findActivos() {
        List<Productos> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT p FROM Productos p LEFT JOIN FETCH p.categoria WHERE p.enabled = true ORDER BY p.nombre ASC",
                    Productos.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findActivos", ex);
        }
        return lista;
    }

    /**
     * Búsqueda exacta por código de barras (para lector láser USB y cámara escáner en Cajero).
     */
    public Productos findByCodigoBarra(String codigoBarra) {
        if (codigoBarra == null || codigoBarra.isBlank()) {
            return null;
        }
        try {
            List<Productos> res = this.entityManager.createQuery(
                    "SELECT p FROM Productos p LEFT JOIN FETCH p.categoria WHERE p.enabled = true AND p.codigoBarra = :codigo",
                    Productos.class
            ).setParameter("codigo", codigoBarra.trim())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByCodigoBarra", ex);
            return null;
        }
    }

    public Productos findByCodigoBarraAnyStatus(String codigoBarra) {
        if (codigoBarra == null || codigoBarra.isBlank()) {
            return null;
        }
        try {
            List<Productos> res = this.entityManager.createQuery(
                    "SELECT p FROM Productos p WHERE p.codigoBarra = :codigo",
                    Productos.class
            ).setParameter("codigo", codigoBarra.trim())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByCodigoBarraAnyStatus", ex);
            return null;
        }
    }

    public Productos findBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        try {
            List<Productos> res = this.entityManager.createQuery(
                    "SELECT p FROM Productos p WHERE UPPER(p.sku) = :sku",
                    Productos.class
            ).setParameter("sku", sku.trim().toUpperCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findBySku", ex);
            return null;
        }
    }

    /**
     * Búsqueda en vivo por nombre, SKU, código de barras o categoría para el Catálogo Rápido del Cajero.
     */
    public List<Productos> buscarPorNombreOCodigo(String termino, int limite) {
        List<Productos> lista = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT p FROM Productos p LEFT JOIN FETCH p.categoria WHERE p.enabled = true ");

            boolean tieneTermino = (termino != null && !termino.isBlank());
            if (tieneTermino) {
                jpql.append(" AND (LOWER(p.nombre) LIKE :q OR LOWER(p.sku) LIKE :q OR LOWER(p.codigoBarra) LIKE :q OR LOWER(p.categoria.nombre) LIKE :q) ");
            }
            jpql.append(" ORDER BY p.nombre ASC ");

            TypedQuery<Productos> q = this.entityManager.createQuery(jpql.toString(), Productos.class);
            if (tieneTermino) {
                q.setParameter("q", "%" + termino.trim().toLowerCase() + "%");
            }
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "buscarPorNombreOCodigo", ex);
        }
        return lista;
    }

    public long contarActivos() {
        try {
            return this.entityManager.createQuery(
                    "SELECT COUNT(p) FROM Productos p WHERE p.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "contarActivos", ex);
            return 0L;
        }
    }

    public long contarStockBajo() {
        try {
            return this.entityManager.createQuery(
                    "SELECT COUNT(p) FROM Productos p WHERE p.enabled = true AND p.stockActual <= p.stockMinimo", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "contarStockBajo", ex);
            return 0L;
        }
    }

    public List<Productos> findStockBajo(int limite) {
        List<Productos> lista = new ArrayList<>();
        try {
            TypedQuery<Productos> q = this.entityManager.createQuery(
                    "SELECT p FROM Productos p LEFT JOIN FETCH p.categoria WHERE p.enabled = true AND p.stockActual <= p.stockMinimo ORDER BY p.stockActual ASC, p.nombre ASC",
                    Productos.class
            );
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findStockBajo", ex);
        }
        return lista;
    }

    public Productos guardarProducto(Productos producto, Integer categoriaId, Users ejecutor, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (producto == null) {
            throw new NegocioException("Datos de producto inválidos.");
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new NegocioException("El nombre del producto es obligatorio.");
        }
        if (producto.getCodigoBarra() == null || producto.getCodigoBarra().isBlank()) {
            throw new NegocioException("El código de barras es obligatorio.");
        }
        if (producto.getSku() == null || producto.getSku().isBlank()) {
            throw new NegocioException("El SKU es obligatorio.");
        }
        if (categoriaId == null) {
            throw new NegocioException("Debes seleccionar una categoría.");
        }
        if (producto.getPrecioVenta() < 0 || producto.getPrecioCosto() < 0) {
            throw new NegocioException("Los precios no pueden ser negativos.");
        }
        if (producto.getStockActual() < 0 || producto.getStockMinimo() < 0) {
            throw new NegocioException("El stock no puede ser negativo.");
        }

        Categorias cat = this.entityManager.find(Categorias.class, categoriaId);
        if (cat == null) {
            throw new NegocioException("La categoría seleccionada no existe.");
        }

        String codigoBarraNorm = producto.getCodigoBarra().trim();
        String skuNorm = producto.getSku().trim().toUpperCase();

        Productos dupBarra = findByCodigoBarraAnyStatus(codigoBarraNorm);
        if (dupBarra != null && (producto.getId() == null || !dupBarra.getId().equals(producto.getId()))) {
            throw new NegocioException("Ya existe un producto con el código de barras '" + codigoBarraNorm + "'.");
        }
        Productos dupSku = findBySku(skuNorm);
        if (dupSku != null && (producto.getId() == null || !dupSku.getId().equals(producto.getId()))) {
            throw new NegocioException("Ya existe un producto con el SKU '" + skuNorm + "'.");
        }

        producto.setCategoria(cat);
        producto.setCodigoBarra(codigoBarraNorm);
        producto.setSku(skuNorm);
        producto.setNombre(producto.getNombre().trim());
        if (producto.getUnidadMedida() == null || producto.getUnidadMedida().isBlank()) {
            producto.setUnidadMedida("UN");
        }

        Date now = new Date();
        boolean esNuevo = (producto.getId() == null);
        if (esNuevo) {
            producto.setCreated(now);
            producto.setModified(now);
            producto.setEnabled(true);
            this.entityManager.persist(producto);
            this.entityManager.flush();

            if (producto.getStockActual() > 0 && ejecutor != null && ejecutor.getId() != null) {
                MovimientosStock mov = new MovimientosStock();
                mov.setCreated(now);
                mov.setModified(now);
                mov.setEnabled(true);
                mov.setProducto(producto);
                mov.setUsuario(this.entityManager.getReference(Users.class, ejecutor.getId()));
                mov.setTipo("ENTRADA");
                mov.setCantidad(producto.getStockActual());
                mov.setStockAnterior(0);
                mov.setStockNuevo(producto.getStockActual());
                mov.setReferencia("STOCK-INICIAL");
                mov.setObservacion("Carga inicial al crear producto");
                this.entityManager.persist(mov);
            }
        } else {
            Productos actual = this.entityManager.find(Productos.class, producto.getId());
            if (actual == null) {
                throw new NegocioException("El producto a editar no existe.");
            }
            actual.setCategoria(cat);
            actual.setCodigoBarra(codigoBarraNorm);
            actual.setSku(skuNorm);
            actual.setNombre(producto.getNombre());
            actual.setDescripcion(producto.getDescripcion());
            actual.setPrecioCosto(producto.getPrecioCosto());
            actual.setPrecioVenta(producto.getPrecioVenta());
            actual.setStockMinimo(producto.getStockMinimo());
            actual.setUnidadMedida(producto.getUnidadMedida());
            actual.setEnabled(producto.getEnabled());
            actual.setModified(now);
            producto = this.entityManager.merge(actual);
        }

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "PRODUCTOS",
                esNuevo ? "CREAR_PRODUCTO" : "EDITAR_PRODUCTO",
                "Productos",
                producto.getId(),
                (esNuevo ? "Creó producto " : "Actualizó producto ") + producto.getNombre()
                        + " [SKU: " + producto.getSku() + ", Barra: " + producto.getCodigoBarra()
                        + ", Precio: $" + producto.getPrecioVenta() + "]",
                ipOrigen
        );

        return producto;
    }

    /**
     * Ajusta el stock de un producto de forma atómica registrando el movimiento en el Kardex y el AuditLog.
     */
    public Productos ajustarStock(Integer productoId,
                                  String tipoMovimiento,
                                  int cantidad,
                                  String observacion,
                                  Users ejecutor,
                                  String rolCodigo,
                                  String ipOrigen) throws NegocioException {
        if (productoId == null) {
            throw new NegocioException("Producto no especificado.");
        }
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad del movimiento debe ser mayor a cero.");
        }
        if (ejecutor == null || ejecutor.getId() == null) {
            throw new NegocioException("Usuario responsable no válido.");
        }

        Productos prod = this.entityManager.find(Productos.class, productoId);
        if (prod == null) {
            throw new NegocioException("Producto no encontrado.");
        }

        String tipo = (tipoMovimiento != null ? tipoMovimiento.trim().toUpperCase() : "ENTRADA");
        boolean esSalida = "AJUSTE_NEGATIVO".equals(tipo) || "MERMA".equals(tipo) || "SALIDA_VENTA".equals(tipo);

        int stockAnterior = prod.getStockActual();
        int stockNuevo = esSalida ? (stockAnterior - cantidad) : (stockAnterior + cantidad);

        if (stockNuevo < 0) {
            throw new NegocioException("Stock insuficiente para descontar " + cantidad
                    + " unidades (stock actual: " + stockAnterior + ").");
        }

        Date now = new Date();
        prod.setStockActual(stockNuevo);
        prod.setModified(now);
        this.entityManager.merge(prod);

        MovimientosStock mov = new MovimientosStock();
        mov.setCreated(now);
        mov.setModified(now);
        mov.setEnabled(true);
        mov.setProducto(prod);
        mov.setUsuario(this.entityManager.getReference(Users.class, ejecutor.getId()));
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setStockAnterior(stockAnterior);
        mov.setStockNuevo(stockNuevo);
        mov.setReferencia("AJUSTE-MANUAL");
        mov.setObservacion(observacion != null ? observacion.trim() : null);
        this.entityManager.persist(mov);

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "INVENTARIO",
                "AJUSTE_STOCK",
                "Productos",
                prod.getId(),
                tipo + " de " + cantidad + " u. en '" + prod.getNombre() + "' (" + stockAnterior + " -> " + stockNuevo + ")"
                        + (observacion != null && !observacion.isBlank() ? ". Obs: " + observacion.trim() : ""),
                ipOrigen
        );

        return prod;
    }

    @Override
    public List<Productos> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Productos> lista = new ArrayList<>();
        try {
            TypedQuery<Productos> q = this.entityManager.createQuery(
                    "SELECT p FROM Productos p LEFT JOIN FETCH p.categoria ORDER BY p.nombre ASC", Productos.class
            );
            q.setFirstResult(first);
            q.setMaxResults(pageSize);
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll(paginado)", ex);
        }
        return lista;
    }

    @Override
    public Long count(Map<String, FilterMeta> filterBy) {
        return contarActivos();
    }
}
