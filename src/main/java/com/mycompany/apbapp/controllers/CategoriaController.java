package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Categorias;
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
public class CategoriaController extends AbstractDao<Categorias> {

    @Inject
    private AuditLogController auditLogController;

    public CategoriaController() {
        super(Categorias.class);
    }

    @Override
    public List<Categorias> findAll() {
        List<Categorias> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Categorias c ORDER BY c.nombre ASC", Categorias.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll", ex);
        }
        return lista;
    }

    public List<Categorias> findActivas() {
        List<Categorias> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT c FROM Categorias c WHERE c.enabled = true ORDER BY c.nombre ASC", Categorias.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findActivas", ex);
        }
        return lista;
    }

    public Categorias findByCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        try {
            List<Categorias> res = this.entityManager.createQuery(
                    "SELECT c FROM Categorias c WHERE UPPER(c.codigo) = :codigo", Categorias.class
            ).setParameter("codigo", codigo.trim().toUpperCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByCodigo", ex);
            return null;
        }
    }

    public Categorias guardarCategoria(Categorias categoria, Users ejecutor, String rolCodigo, String ipOrigen)
            throws NegocioException {
        if (categoria == null || categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new NegocioException("El nombre de la categoría es obligatorio.");
        }
        if (categoria.getCodigo() == null || categoria.getCodigo().isBlank()) {
            throw new NegocioException("El código de la categoría es obligatorio.");
        }

        String codigoNorm = categoria.getCodigo().trim().toUpperCase().replaceAll("\\s+", "_");
        categoria.setCodigo(codigoNorm);
        categoria.setNombre(categoria.getNombre().trim());

        Categorias existente = findByCodigo(codigoNorm);
        if (existente != null && (categoria.getId() == null || !existente.getId().equals(categoria.getId()))) {
            throw new NegocioException("Ya existe una categoría con el código '" + codigoNorm + "'.");
        }

        Date now = new Date();
        boolean esNueva = (categoria.getId() == null);
        if (esNueva) {
            categoria.setCreated(now);
            categoria.setModified(now);
            categoria.setEnabled(true);
            this.entityManager.persist(categoria);
            this.entityManager.flush();
        } else {
            categoria.setModified(now);
            categoria = this.entityManager.merge(categoria);
        }

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "PRODUCTOS",
                esNueva ? "CREAR_CATEGORIA" : "EDITAR_CATEGORIA",
                "Categorias",
                categoria.getId(),
                (esNueva ? "Creó categoría " : "Actualizó categoría ") + categoria.getNombre() + " [" + categoria.getCodigo() + "]",
                ipOrigen
        );

        return categoria;
    }

    @Override
    public List<Categorias> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Categorias> lista = new ArrayList<>();
        try {
            TypedQuery<Categorias> q = this.entityManager.createQuery(
                    "SELECT c FROM Categorias c ORDER BY c.nombre ASC", Categorias.class
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
            return this.entityManager.createQuery("SELECT COUNT(c) FROM Categorias c", Long.class).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }
}
