package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.CajaTurnos;
import com.mycompany.apbapp.entities.Clientes;
import com.mycompany.apbapp.entities.MovimientosStock;
import com.mycompany.apbapp.entities.Productos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.entities.VentaDetalles;
import com.mycompany.apbapp.entities.Ventas;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.TypedQuery;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class VentaController extends AbstractDao<Ventas> {

    @Inject
    private AuditLogController auditLogController;

    public VentaController() {
        super(Ventas.class);
    }

    /**
     * Registra una venta completa dentro de una transacción JTA atómica:
     * 1) Valida caja abierta y stock suficiente por cada ítem.
     * 2) Crea Ventas + VentaDetalles con snapshot de nombre/código/precio.
     * 3) Descuenta Productos.stockActual y registra MovimientosStock (SALIDA_VENTA).
     * 4) Acumula el monto en CajaTurnos según el método de pago.
     * 5) Registra el evento en AuditLogs.
     */
    public Ventas registrarVenta(Integer cajaTurnoId,
                                 Users vendedor,
                                 Integer clienteId,
                                 List<VentaDetalles> itemsCarrito,
                                 String metodoPago,
                                 int descuento,
                                 int montoPagado,
                                 String observacion,
                                 String rolCodigo,
                                 String ipOrigen) throws NegocioException {

        if (vendedor == null || vendedor.getId() == null) {
            throw new NegocioException("No hay un vendedor autenticado.");
        }
        if (cajaTurnoId == null) {
            throw new NegocioException("Debes abrir un turno de caja con valor inicial antes de registrar ventas.");
        }
        if (itemsCarrito == null || itemsCarrito.isEmpty()) {
            throw new NegocioException("El ticket no contiene productos.");
        }
        if (descuento < 0) {
            throw new NegocioException("El descuento no puede ser negativo.");
        }

        CajaTurnos turno = this.entityManager.find(CajaTurnos.class, cajaTurnoId);
        if (turno == null || !turno.isAbierta()) {
            throw new NegocioException("El turno de caja seleccionado no está abierto.");
        }

        String metodo = (metodoPago != null && !metodoPago.isBlank()) ? metodoPago.trim().toUpperCase() : "EFECTIVO";
        Date now = new Date();
        String folio = generarFolio(now);

        Ventas venta = new Ventas();
        venta.setCreated(now);
        venta.setModified(now);
        venta.setEnabled(true);
        venta.setFolio(folio);
        venta.setUsuario(this.entityManager.getReference(Users.class, vendedor.getId()));
        venta.setCajaTurno(turno);
        if (clienteId != null) {
            Clientes cli = this.entityManager.find(Clientes.class, clienteId);
            if (cli != null) {
                venta.setCliente(cli);
            }
        }
        venta.setMetodoPago(metodo);
        venta.setEstado("COMPLETADA");
        venta.setObservacion(observacion != null && !observacion.isBlank() ? observacion.trim() : null);

        int subtotalGeneral = 0;
        int totalUnidades = 0;
        List<MovimientosStock> movimientos = new ArrayList<>();

        for (VentaDetalles item : itemsCarrito) {
            if (item == null || item.getProducto() == null || item.getProducto().getId() == null) {
                throw new NegocioException("Producto inválido en el ticket.");
            }
            if (item.getCantidad() <= 0) {
                throw new NegocioException("La cantidad de cada producto debe ser mayor a cero.");
            }

            Productos prod = this.entityManager.find(Productos.class, item.getProducto().getId());
            if (prod == null || !prod.getEnabled()) {
                throw new NegocioException("Uno de los productos del ticket ya no está disponible.");
            }
            if (prod.getStockActual() < item.getCantidad()) {
                throw new NegocioException("Stock insuficiente para '" + prod.getNombre()
                        + "' (disponible: " + prod.getStockActual() + ", solicitado: " + item.getCantidad() + ").");
            }

            int precioUnit = prod.getPrecioVenta();
            int subtotalLinea = precioUnit * item.getCantidad();
            subtotalGeneral += subtotalLinea;
            totalUnidades += item.getCantidad();

            VentaDetalles det = new VentaDetalles();
            det.setCreated(now);
            det.setModified(now);
            det.setEnabled(true);
            det.setProducto(prod);
            det.setProductoNombre(prod.getNombre());
            det.setCodigoBarra(prod.getCodigoBarra());
            det.setCantidad(item.getCantidad());
            det.setPrecioUnitario(precioUnit);
            det.setSubtotal(subtotalLinea);
            venta.addDetalle(det);

            // Descuento de stock y preparacion de Kardex
            int stockAnterior = prod.getStockActual();
            int stockNuevo = stockAnterior - item.getCantidad();
            prod.setStockActual(stockNuevo);
            prod.setModified(now);
            this.entityManager.merge(prod);

            MovimientosStock mov = new MovimientosStock();
            mov.setCreated(now);
            mov.setModified(now);
            mov.setEnabled(true);
            mov.setProducto(prod);
            mov.setUsuario(this.entityManager.getReference(Users.class, vendedor.getId()));
            mov.setTipo("SALIDA_VENTA");
            mov.setCantidad(item.getCantidad());
            mov.setStockAnterior(stockAnterior);
            mov.setStockNuevo(stockNuevo);
            mov.setReferencia(folio);
            mov.setObservacion("Venta " + folio + " (" + metodo + ")");
            movimientos.add(mov);
        }

        if (descuento > subtotalGeneral) {
            throw new NegocioException("El descuento no puede superar el subtotal de la venta.");
        }

        int totalFinal = subtotalGeneral - descuento;
        int pagadoFinal = "EFECTIVO".equals(metodo) ? montoPagado : totalFinal;
        if ("EFECTIVO".equals(metodo) && pagadoFinal < totalFinal) {
            throw new NegocioException("El monto pagado en efectivo (" + Util.formatoCLP(pagadoFinal)
                    + ") es inferior al total (" + Util.formatoCLP(totalFinal) + ").");
        }
        int vuelto = Math.max(0, pagadoFinal - totalFinal);

        venta.setSubtotal(subtotalGeneral);
        venta.setDescuento(descuento);
        venta.setTotal(totalFinal);
        venta.setMontoPagado(pagadoFinal);
        venta.setVuelto(vuelto);

        this.entityManager.persist(venta);
        this.entityManager.flush();

        for (MovimientosStock mov : movimientos) {
            this.entityManager.persist(mov);
        }

        acumularEnCaja(turno, metodo, totalFinal);
        turno.setModified(now);
        this.entityManager.merge(turno);

        String clienteInfo = venta.getCliente() != null
                ? " | Cliente: " + venta.getCliente().getRut()
                : " | Consumidor Final";
        auditLogController.registrar(
                vendedor,
                rolCodigo,
                "VENTAS",
                "REGISTRAR_VENTA",
                "Ventas",
                venta.getId(),
                "Venta " + folio + " por " + Util.formatoCLP(totalFinal)
                        + " (" + totalUnidades + " u., " + metodo + ")" + clienteInfo,
                ipOrigen
        );

        return venta;
    }

    /**
     * Anula una venta completada devolviendo automáticamente el stock y ajustando el turno de caja.
     */
    public Ventas anularVenta(Integer ventaId, Users ejecutor, String motivo, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (ventaId == null) {
            throw new NegocioException("Venta no especificada.");
        }
        Ventas venta = findConDetallesById(ventaId);
        if (venta == null) {
            throw new NegocioException("Venta no encontrada.");
        }
        if ("ANULADA".equalsIgnoreCase(venta.getEstado())) {
            throw new NegocioException("Esta venta ya fue anulada.");
        }

        Date now = new Date();
        for (VentaDetalles det : venta.getDetalles()) {
            Productos prod = this.entityManager.find(Productos.class, det.getProducto().getId());
            if (prod != null) {
                int anterior = prod.getStockActual();
                int nuevo = anterior + det.getCantidad();
                prod.setStockActual(nuevo);
                prod.setModified(now);
                this.entityManager.merge(prod);

                MovimientosStock mov = new MovimientosStock();
                mov.setCreated(now);
                mov.setModified(now);
                mov.setEnabled(true);
                mov.setProducto(prod);
                mov.setUsuario(this.entityManager.getReference(Users.class, ejecutor.getId()));
                mov.setTipo("DEVOLUCION");
                mov.setCantidad(det.getCantidad());
                mov.setStockAnterior(anterior);
                mov.setStockNuevo(nuevo);
                mov.setReferencia(venta.getFolio());
                mov.setObservacion("Anulación de venta " + venta.getFolio()
                        + (motivo != null && !motivo.isBlank() ? ": " + motivo.trim() : ""));
                this.entityManager.persist(mov);
            }
        }

        CajaTurnos turno = this.entityManager.find(CajaTurnos.class, venta.getCajaTurno().getId());
        if (turno != null) {
            acumularEnCaja(turno, venta.getMetodoPago(), -venta.getTotal());
            turno.setModified(now);
            this.entityManager.merge(turno);
        }

        venta.setEstado("ANULADA");
        venta.setModified(now);
        if (motivo != null && !motivo.isBlank()) {
            venta.setObservacion("ANULADA: " + motivo.trim());
        }
        this.entityManager.merge(venta);

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "VENTAS",
                "ANULAR_VENTA",
                "Ventas",
                venta.getId(),
                "Anuló venta " + venta.getFolio() + " (" + Util.formatoCLP(venta.getTotal()) + ")"
                        + (motivo != null && !motivo.isBlank() ? ". Motivo: " + motivo.trim() : ""),
                ipOrigen
        );

        return venta;
    }

    private void acumularEnCaja(CajaTurnos turno, String metodoPago, int delta) {
        switch (metodoPago) {
            case "EFECTIVO":
                turno.setTotalVentasEfectivo(Math.max(0, turno.getTotalVentasEfectivo() + delta));
                break;
            case "DEBITO":
                turno.setTotalVentasDebito(Math.max(0, turno.getTotalVentasDebito() + delta));
                break;
            case "CREDITO":
                turno.setTotalVentasCredito(Math.max(0, turno.getTotalVentasCredito() + delta));
                break;
            case "TRANSFERENCIA":
                turno.setTotalVentasTransferencia(Math.max(0, turno.getTotalVentasTransferencia() + delta));
                break;
            default:
                turno.setTotalVentasEfectivo(Math.max(0, turno.getTotalVentasEfectivo() + delta));
                break;
        }
        turno.recalcularEsperado();
    }

    private String generarFolio(Date now) {
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(now);
        int rand = ThreadLocalRandom.current().nextInt(100, 1000);
        return "VTA-" + ts + "-" + rand;
    }

    public Ventas findConDetallesById(Integer ventaId) {
        if (ventaId == null) {
            return null;
        }
        try {
            List<Ventas> res = this.entityManager.createQuery(
                    "SELECT DISTINCT v FROM Ventas v LEFT JOIN FETCH v.usuario LEFT JOIN FETCH v.cliente LEFT JOIN FETCH v.cajaTurno LEFT JOIN FETCH v.detalles d LEFT JOIN FETCH d.producto WHERE v.id = :id",
                    Ventas.class
            ).setParameter("id", ventaId).getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findConDetallesById", ex);
            return null;
        }
    }

    public List<Ventas> findRecientes(int limite) {
        List<Ventas> lista = new ArrayList<>();
        try {
            TypedQuery<Ventas> q = this.entityManager.createQuery(
                    "SELECT v FROM Ventas v LEFT JOIN FETCH v.usuario LEFT JOIN FETCH v.cliente LEFT JOIN FETCH v.cajaTurno WHERE v.enabled = true ORDER BY v.created DESC, v.id DESC",
                    Ventas.class
            );
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findRecientes", ex);
        }
        return lista;
    }

    public List<Ventas> buscarVentas(String folio, String metodoPago, String estado, Integer userId, int limite) {
        List<Ventas> lista = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT v FROM Ventas v LEFT JOIN FETCH v.usuario LEFT JOIN FETCH v.cliente LEFT JOIN FETCH v.cajaTurno WHERE v.enabled = true ");
            if (folio != null && !folio.isBlank()) {
                jpql.append(" AND LOWER(v.folio) LIKE :folio ");
            }
            if (metodoPago != null && !metodoPago.isBlank() && !"TODOS".equalsIgnoreCase(metodoPago)) {
                jpql.append(" AND v.metodoPago = :metodo ");
            }
            if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
                jpql.append(" AND v.estado = :estado ");
            }
            if (userId != null) {
                jpql.append(" AND v.usuario.id = :uid ");
            }
            jpql.append(" ORDER BY v.created DESC, v.id DESC ");

            TypedQuery<Ventas> q = this.entityManager.createQuery(jpql.toString(), Ventas.class);
            if (folio != null && !folio.isBlank()) {
                q.setParameter("folio", "%" + folio.trim().toLowerCase() + "%");
            }
            if (metodoPago != null && !metodoPago.isBlank() && !"TODOS".equalsIgnoreCase(metodoPago)) {
                q.setParameter("metodo", metodoPago.trim().toUpperCase());
            }
            if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
                q.setParameter("estado", estado.trim().toUpperCase());
            }
            if (userId != null) {
                q.setParameter("uid", userId);
            }
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "buscarVentas", ex);
        }
        return lista;
    }

    public long sumarVentasHoy() {
        try {
            Date inicio = inicioDeHoy();
            Date fin = finDeHoy();
            Long suma = this.entityManager.createQuery(
                    "SELECT COALESCE(SUM(v.total), 0) FROM Ventas v WHERE v.enabled = true AND v.estado = 'COMPLETADA' AND v.created >= :inicio AND v.created < :fin",
                    Long.class
            ).setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getSingleResult();
            return suma != null ? suma : 0L;
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "sumarVentasHoy", ex);
            return 0L;
        }
    }

    public long contarVentasHoy() {
        try {
            Date inicio = inicioDeHoy();
            Date fin = finDeHoy();
            Long total = this.entityManager.createQuery(
                    "SELECT COUNT(v) FROM Ventas v WHERE v.enabled = true AND v.estado = 'COMPLETADA' AND v.created >= :inicio AND v.created < :fin",
                    Long.class
            ).setParameter("inicio", inicio)
                    .setParameter("fin", fin)
                    .getSingleResult();
            return total != null ? total : 0L;
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "contarVentasHoy", ex);
            return 0L;
        }
    }

    public long contarVentasPorCliente(Integer clienteId) {
        if (clienteId == null) {
            return 0L;
        }
        try {
            Long total = this.entityManager.createQuery(
                    "SELECT COUNT(v) FROM Ventas v WHERE v.enabled = true AND v.estado = 'COMPLETADA' AND v.cliente.id = :cid",
                    Long.class
            ).setParameter("cid", clienteId).getSingleResult();
            return total != null ? total : 0L;
        } catch (Exception ex) {
            return 0L;
        }
    }

    public long sumarComprasPorCliente(Integer clienteId) {
        if (clienteId == null) {
            return 0L;
        }
        try {
            Long total = this.entityManager.createQuery(
                    "SELECT COALESCE(SUM(v.total), 0) FROM Ventas v WHERE v.enabled = true AND v.estado = 'COMPLETADA' AND v.cliente.id = :cid",
                    Long.class
            ).setParameter("cid", clienteId).getSingleResult();
            return total != null ? total : 0L;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private Date inicioDeHoy() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date finDeHoy() {
        return Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public List<Ventas> findAll() {
        return findRecientes(150);
    }

    @Override
    public List<Ventas> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Ventas> lista = new ArrayList<>();
        try {
            TypedQuery<Ventas> q = this.entityManager.createQuery(
                    "SELECT v FROM Ventas v LEFT JOIN FETCH v.usuario LEFT JOIN FETCH v.cliente LEFT JOIN FETCH v.cajaTurno ORDER BY v.created DESC",
                    Ventas.class
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
        try {
            return this.entityManager.createQuery(
                    "SELECT COUNT(v) FROM Ventas v WHERE v.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }
}
