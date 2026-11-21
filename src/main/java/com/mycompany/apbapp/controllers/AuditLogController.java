package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.AuditLogs;
import com.mycompany.apbapp.entities.Users;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class AuditLogController extends AbstractDao<AuditLogs> {

    public AuditLogController() {
        super(AuditLogs.class);
    }

    /**
     * Registra un evento auditable en una transacción independiente (REQUIRES_NEW)
     * para asegurar trazabilidad incluso si se invoca fuera de una operación CRUD.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void registrar(Users user,
                          String rolCodigo,
                          String modulo,
                          String accion,
                          String entidad,
                          Integer entidadId,
                          String detalle,
                          String ipOrigen) {
        try {
            AuditLogs log = new AuditLogs();
            Date now = new Date();
            log.setCreated(now);
            log.setModified(now);
            log.setEnabled(true);

            if (user != null && user.getId() != null) {
                log.setUsuario(this.entityManager.getReference(Users.class, user.getId()));
                log.setUsername(recortar(user.getUsername() != null ? user.getUsername() : "usuario#" + user.getId(), 60));
            } else {
                log.setUsername("SYSTEM");
            }

            log.setRolCodigo(recortar((rolCodigo != null && !rolCodigo.isBlank()) ? rolCodigo.toUpperCase() : "SISTEMA", 30));
            log.setModulo(recortar((modulo != null && !modulo.isBlank()) ? modulo.toUpperCase() : "GENERAL", 40));
            log.setAccion(recortar((accion != null && !accion.isBlank()) ? accion.toUpperCase() : "ACCION", 60));
            log.setEntidad(entidad != null ? recortar(entidad, 60) : null);
            log.setEntidadId(entidadId);
            log.setDetalle(recortar((detalle != null && !detalle.isBlank()) ? detalle : accion, 500));
            log.setIpOrigen(ipOrigen != null ? recortar(ipOrigen, 64) : null);

            this.entityManager.persist(log);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "registrar", ex);
        }
    }

    public List<AuditLogs> buscarLogs(String modulo,
                                      String username,
                                      String texto,
                                      Date desde,
                                      Date hasta,
                                      int limite) {
        List<AuditLogs> lista = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT a FROM AuditLogs a LEFT JOIN FETCH a.usuario WHERE a.enabled = true ");

            if (modulo != null && !modulo.isBlank() && !"TODOS".equalsIgnoreCase(modulo)) {
                jpql.append(" AND a.modulo = :modulo ");
            }
            if (username != null && !username.isBlank()) {
                jpql.append(" AND LOWER(a.username) LIKE :username ");
            }
            if (texto != null && !texto.isBlank()) {
                jpql.append(" AND (LOWER(a.accion) LIKE :texto OR LOWER(a.detalle) LIKE :texto OR LOWER(COALESCE(a.entidad, '')) LIKE :texto) ");
            }
            if (desde != null) {
                jpql.append(" AND a.created >= :desde ");
            }
            if (hasta != null) {
                jpql.append(" AND a.created <= :hasta ");
            }
            jpql.append(" ORDER BY a.created DESC, a.id DESC ");

            TypedQuery<AuditLogs> query = this.entityManager.createQuery(jpql.toString(), AuditLogs.class);

            if (modulo != null && !modulo.isBlank() && !"TODOS".equalsIgnoreCase(modulo)) {
                query.setParameter("modulo", modulo.trim().toUpperCase());
            }
            if (username != null && !username.isBlank()) {
                query.setParameter("username", "%" + username.trim().toLowerCase() + "%");
            }
            if (texto != null && !texto.isBlank()) {
                query.setParameter("texto", "%" + texto.trim().toLowerCase() + "%");
            }
            if (desde != null) {
                query.setParameter("desde", desde);
            }
            if (hasta != null) {
                query.setParameter("hasta", hasta);
            }
            if (limite > 0) {
                query.setMaxResults(limite);
            }

            lista = query.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "buscarLogs", ex);
        }
        return lista;
    }

    public List<String> listarModulosDistintos() {
        List<String> modulos = new ArrayList<>();
        try {
            modulos = this.entityManager.createQuery(
                    "SELECT DISTINCT a.modulo FROM AuditLogs a WHERE a.enabled = true ORDER BY a.modulo ASC",
                    String.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "listarModulosDistintos", ex);
        }
        return modulos;
    }

    @Override
    public List<AuditLogs> findAll() {
        return buscarLogs(null, null, null, null, null, 200);
    }

    @Override
    public List<AuditLogs> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<AuditLogs> lista = new ArrayList<>();
        try {
            TypedQuery<AuditLogs> query = this.entityManager.createQuery(
                    "SELECT a FROM AuditLogs a LEFT JOIN FETCH a.usuario WHERE a.enabled = true ORDER BY a.created DESC, a.id DESC",
                    AuditLogs.class
            );
            query.setFirstResult(first);
            query.setMaxResults(pageSize);
            lista = query.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll(paginado)", ex);
        }
        return lista;
    }

    @Override
    public Long count(Map<String, FilterMeta> filterBy) {
        try {
            return this.entityManager.createQuery(
                    "SELECT COUNT(a) FROM AuditLogs a WHERE a.enabled = true", Long.class
            ).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String clean = texto.trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
