package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Cargos;
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
public class CargoController extends AbstractDao<Cargos> {

    @Inject
    private AuditLogController auditLogController;

    public CargoController() {
        super(Cargos.class);
    }

    @Override
    public List<Cargos> findAll() {
        List<Cargos> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Cargos c ORDER BY c.id ASC", Cargos.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll", ex);
        }
        return lista;
    }

    public List<Cargos> findActivos() {
        List<Cargos> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Cargos c WHERE c.enabled = true ORDER BY c.nombre ASC", Cargos.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findActivos", ex);
        }
        return lista;
    }

    public Cargos findByCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            List<Cargos> res = this.entityManager.createQuery(
                    "SELECT c FROM Cargos c WHERE UPPER(c.codigo) = :codigo", Cargos.class
            ).setParameter("codigo", codigo.trim().toUpperCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByCodigo", ex);
            return null;
        }
    }

    public Cargos guardarCargo(Cargos cargo, Users ejecutor, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (cargo == null) {
            throw new NegocioException("Datos de cargo inválidos.");
        }
        if (cargo.getNombre() == null || cargo.getNombre().isBlank()) {
            throw new NegocioException("El nombre del cargo es obligatorio.");
        }
        if (cargo.getCodigo() == null || cargo.getCodigo().isBlank()) {
            throw new NegocioException("El código del cargo es obligatorio.");
        }

        String codigoNorm = cargo.getCodigo().trim().toUpperCase().replaceAll("\\s+", "_");
        if (!codigoNorm.matches("^[A-Z0-9_]{2,50}$")) {
            throw new NegocioException("El código solo puede contener letras, números y guion bajo (ej: TRABAJADOR, CAJERO).");
        }

        Cargos existente = findByCodigo(codigoNorm);
        if (existente != null && (cargo.getId() == null || !existente.getId().equals(cargo.getId()))) {
            throw new NegocioException("Ya existe un cargo con el código '" + codigoNorm + "'.");
        }

        cargo.setCodigo(codigoNorm);
        cargo.setNombre(cargo.getNombre().trim());
        Date now = new Date();
        boolean esNuevo = (cargo.getId() == null);

        if (esNuevo) {
            cargo.setCreated(now);
            cargo.setModified(now);
            cargo.setEnabled(true);
            this.entityManager.persist(cargo);
            this.entityManager.flush();
        } else {
            cargo.setModified(now);
            cargo = this.entityManager.merge(cargo);
        }

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "USUARIOS",
                esNuevo ? "CREAR_CARGO" : "EDITAR_CARGO",
                "Cargos",
                cargo.getId(),
                (esNuevo ? "Creó cargo " : "Actualizó cargo ") + cargo.getNombre() + " [" + cargo.getCodigo() + "]",
                ipOrigen
        );

        return cargo;
    }

    @Override
    public List<Cargos> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Cargos> lista = new ArrayList<>();
        try {
            TypedQuery<Cargos> q = this.entityManager.createQuery(
                    "SELECT c FROM Cargos c ORDER BY c.id ASC", Cargos.class
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
            return this.entityManager.createQuery("SELECT COUNT(c) FROM Cargos c", Long.class).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }
}
