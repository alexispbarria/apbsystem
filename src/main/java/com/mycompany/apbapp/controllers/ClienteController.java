package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Clientes;
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
public class ClienteController extends AbstractDao<Clientes> {

    @Inject
    private AuditLogController auditLogController;

    public ClienteController() {
        super(Clientes.class);
    }

    @Override
    public List<Clientes> findAll() {
        List<Clientes> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Clientes c ORDER BY c.nombreCompleto ASC", Clientes.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll", ex);
        }
        return lista;
    }

    public List<Clientes> findActivos() {
        List<Clientes> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Clientes c WHERE c.enabled = true ORDER BY c.nombreCompleto ASC", Clientes.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findActivos", ex);
        }
        return lista;
    }

    public Clientes findByRut(String rut) {
        if (rut == null || rut.isBlank()) {
            return null;
        }
        String norm = Util.formatearRUT(rut);
        if (norm == null) {
            return null;
        }
        try {
            List<Clientes> res = this.entityManager.createQuery(
                    "SELECT c FROM Clientes c WHERE c.rut = :rut", Clientes.class
            ).setParameter("rut", norm)
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByRut", ex);
            return null;
        }
    }

    public List<Clientes> buscar(String termino, int limite) {
        List<Clientes> lista = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder("SELECT c FROM Clientes c WHERE c.enabled = true ");
            boolean tiene = termino != null && !termino.isBlank();
            if (tiene) {
                jpql.append(" AND (LOWER(c.nombreCompleto) LIKE :q OR LOWER(c.rut) LIKE :q OR LOWER(COALESCE(c.email, '')) LIKE :q) ");
            }
            jpql.append(" ORDER BY c.nombreCompleto ASC ");
            TypedQuery<Clientes> q = this.entityManager.createQuery(jpql.toString(), Clientes.class);
            if (tiene) {
                q.setParameter("q", "%" + termino.trim().toLowerCase() + "%");
            }
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "buscar", ex);
        }
        return lista;
    }

    public long contarActivos() {
        try {
            return this.entityManager.createQuery(
                    "SELECT COUNT(c) FROM Clientes c WHERE c.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "contarActivos", ex);
            return 0L;
        }
    }

    public Clientes guardarCliente(Clientes cliente, Users ejecutor, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (cliente == null) {
            throw new NegocioException("Datos de cliente inválidos.");
        }
        String rutNorm = Util.formatearRUT(cliente.getRut());
        if (rutNorm == null || !Util.validarRut(rutNorm)) {
            throw new NegocioException("RUT inválido. Debe seguir la convención xxxxxxxx-x y tener dígito verificador correcto.");
        }
        if (cliente.getNombreCompleto() == null || cliente.getNombreCompleto().isBlank()) {
            throw new NegocioException("El nombre del cliente es obligatorio.");
        }

        Clientes dup = findByRut(rutNorm);
        if (dup != null && (cliente.getId() == null || !dup.getId().equals(cliente.getId()))) {
            throw new NegocioException("Ya existe un cliente registrado con el RUT " + rutNorm + ".");
        }

        cliente.setRut(rutNorm);
        cliente.setNombreCompleto(cliente.getNombreCompleto().trim());
        Date now = new Date();
        boolean esNuevo = (cliente.getId() == null);

        if (esNuevo) {
            cliente.setCreated(now);
            cliente.setModified(now);
            cliente.setEnabled(true);
            this.entityManager.persist(cliente);
            this.entityManager.flush();
        } else {
            cliente.setModified(now);
            cliente = this.entityManager.merge(cliente);
        }

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "CLIENTES",
                esNuevo ? "CREAR_CLIENTE" : "EDITAR_CLIENTE",
                "Clientes",
                cliente.getId(),
                (esNuevo ? "Registró cliente " : "Actualizó cliente ") + cliente.getNombreCompleto() + " (RUT: " + cliente.getRut() + ")",
                ipOrigen
        );

        return cliente;
    }

    @Override
    public List<Clientes> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Clientes> lista = new ArrayList<>();
        try {
            TypedQuery<Clientes> q = this.entityManager.createQuery(
                    "SELECT c FROM Clientes c ORDER BY c.nombreCompleto ASC", Clientes.class
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
