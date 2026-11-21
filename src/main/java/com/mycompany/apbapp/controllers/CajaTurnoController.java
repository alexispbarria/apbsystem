package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.CajaTurnos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
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
public class CajaTurnoController extends AbstractDao<CajaTurnos> {

    @Inject
    private AuditLogController auditLogController;

    public CajaTurnoController() {
        super(CajaTurnos.class);
    }

    public CajaTurnos findCajaAbiertaPorUsuario(Integer userId) {
        if (userId == null) {
            return null;
        }
        try {
            List<CajaTurnos> res = this.entityManager.createQuery(
                    "SELECT c FROM CajaTurnos c LEFT JOIN FETCH c.usuario WHERE c.enabled = true AND c.estado = 'ABIERTA' AND c.usuario.id = :uid ORDER BY c.fechaApertura DESC",
                    CajaTurnos.class
            ).setParameter("uid", userId)
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findCajaAbiertaPorUsuario", ex);
            return null;
        }
    }

    public CajaTurnos findUltimaCajaAbierta() {
        try {
            List<CajaTurnos> res = this.entityManager.createQuery(
                    "SELECT c FROM CajaTurnos c LEFT JOIN FETCH c.usuario WHERE c.enabled = true AND c.estado = 'ABIERTA' ORDER BY c.fechaApertura DESC",
                    CajaTurnos.class
            ).setMaxResults(1).getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findUltimaCajaAbierta", ex);
            return null;
        }
    }

    public List<CajaTurnos> findRecientes(int limite) {
        List<CajaTurnos> lista = new ArrayList<>();
        try {
            TypedQuery<CajaTurnos> q = this.entityManager.createQuery(
                    "SELECT c FROM CajaTurnos c LEFT JOIN FETCH c.usuario WHERE c.enabled = true ORDER BY c.fechaApertura DESC",
                    CajaTurnos.class
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

    /**
     * Abre un turno de caja registrando el valor de caja inicial y dejando trazabilidad en AuditLogs.
     */
    public CajaTurnos abrirCaja(Users usuario, int montoInicial, String observacion, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (usuario == null || usuario.getId() == null) {
            throw new NegocioException("Usuario no autenticado para abrir caja.");
        }
        if (montoInicial < 0) {
            throw new NegocioException("El valor de caja inicial no puede ser negativo.");
        }
        CajaTurnos abierta = findCajaAbiertaPorUsuario(usuario.getId());
        if (abierta != null) {
            throw new NegocioException("Ya tienes un turno de caja abierto (#" + abierta.getId() + ").");
        }

        Date now = new Date();
        CajaTurnos turno = new CajaTurnos();
        turno.setCreated(now);
        turno.setModified(now);
        turno.setEnabled(true);
        turno.setUsuario(this.entityManager.getReference(Users.class, usuario.getId()));
        turno.setFechaApertura(now);
        turno.setMontoInicial(montoInicial);
        turno.setTotalVentasEfectivo(0);
        turno.setTotalVentasDebito(0);
        turno.setTotalVentasCredito(0);
        turno.setTotalVentasTransferencia(0);
        turno.setMontoEsperadoCierre(montoInicial);
        turno.setEstado("ABIERTA");
        turno.setObservacion(observacion != null && !observacion.isBlank() ? observacion.trim() : null);

        this.entityManager.persist(turno);
        this.entityManager.flush();

        auditLogController.registrar(
                usuario,
                rolCodigo,
                "CAJA",
                "APERTURA_CAJA",
                "CajaTurnos",
                turno.getId(),
                "Abrió turno de caja #" + turno.getId() + " con caja inicial de " + Util.formatoCLP(montoInicial),
                ipOrigen
        );

        return turno;
    }

    /**
     * Cierra el turno de caja activo realizando el arqueo (efectivo contado vs esperado).
     */
    public CajaTurnos cerrarCaja(Integer cajaTurnoId, int montoRealCierre, String observacion, Users ejecutor, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (cajaTurnoId == null) {
            throw new NegocioException("Turno de caja no especificado.");
        }
        if (montoRealCierre < 0) {
            throw new NegocioException("El monto contado al cierre no puede ser negativo.");
        }

        CajaTurnos turno = this.entityManager.find(CajaTurnos.class, cajaTurnoId);
        if (turno == null) {
            throw new NegocioException("Turno de caja no encontrado.");
        }
        if (!turno.isAbierta()) {
            throw new NegocioException("Este turno de caja ya se encuentra cerrado.");
        }

        Date now = new Date();
        turno.recalcularEsperado();
        turno.setFechaCierre(now);
        turno.setMontoRealCierre(montoRealCierre);
        turno.setDiferenciaCierre(montoRealCierre - turno.getMontoEsperadoCierre());
        turno.setEstado("CERRADA");
        turno.setModified(now);
        if (observacion != null && !observacion.isBlank()) {
            turno.setObservacion(observacion.trim());
        }

        this.entityManager.merge(turno);

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "CAJA",
                "CIERRE_CAJA",
                "CajaTurnos",
                turno.getId(),
                "Cerró caja #" + turno.getId()
                        + " | Inicial: " + Util.formatoCLP(turno.getMontoInicial())
                        + " | Esperado efectivo: " + Util.formatoCLP(turno.getMontoEsperadoCierre())
                        + " | Contado: " + Util.formatoCLP(montoRealCierre)
                        + " | Diferencia: " + Util.formatoCLP(turno.getDiferenciaCierre()),
                ipOrigen
        );

        return turno;
    }

    @Override
    public List<CajaTurnos> findAll() {
        return findRecientes(100);
    }

    @Override
    public List<CajaTurnos> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<CajaTurnos> lista = new ArrayList<>();
        try {
            TypedQuery<CajaTurnos> q = this.entityManager.createQuery(
                    "SELECT c FROM CajaTurnos c LEFT JOIN FETCH c.usuario ORDER BY c.fechaApertura DESC",
                    CajaTurnos.class
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
                    "SELECT COUNT(c) FROM CajaTurnos c WHERE c.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }
}
