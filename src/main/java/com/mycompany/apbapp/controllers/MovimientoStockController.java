package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.MovimientosStock;
import jakarta.ejb.Stateless;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class MovimientoStockController extends AbstractDao<MovimientosStock> {

    public MovimientoStockController() {
        super(MovimientosStock.class);
    }

    public List<MovimientosStock> findRecientes(int limite) {
        List<MovimientosStock> lista = new ArrayList<>();
        try {
            TypedQuery<MovimientosStock> q = this.entityManager.createQuery(
                    "SELECT m FROM MovimientosStock m LEFT JOIN FETCH m.producto LEFT JOIN FETCH m.usuario WHERE m.enabled = true ORDER BY m.created DESC, m.id DESC",
                    MovimientosStock.class
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

    public List<MovimientosStock> findByProducto(Integer productoId, int limite) {
        List<MovimientosStock> lista = new ArrayList<>();
        if (productoId == null) {
            return lista;
        }
        try {
            TypedQuery<MovimientosStock> q = this.entityManager.createQuery(
                    "SELECT m FROM MovimientosStock m LEFT JOIN FETCH m.producto LEFT JOIN FETCH m.usuario WHERE m.enabled = true AND m.producto.id = :pid ORDER BY m.created DESC, m.id DESC",
                    MovimientosStock.class
            ).setParameter("pid", productoId);
            if (limite > 0) {
                q.setMaxResults(limite);
            }
            lista = q.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByProducto", ex);
        }
        return lista;
    }

    @Override
    public List<MovimientosStock> findAll() {
        return findRecientes(150);
    }

    @Override
    public List<MovimientosStock> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<MovimientosStock> lista = new ArrayList<>();
        try {
            TypedQuery<MovimientosStock> q = this.entityManager.createQuery(
                    "SELECT m FROM MovimientosStock m LEFT JOIN FETCH m.producto LEFT JOIN FETCH m.usuario WHERE m.enabled = true ORDER BY m.created DESC, m.id DESC",
                    MovimientosStock.class
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
                    "SELECT COUNT(m) FROM MovimientosStock m WHERE m.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }
}
