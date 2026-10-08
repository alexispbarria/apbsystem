package com.mycompany.apbapp.controllers;


import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Users;
import jakarta.ejb.Stateless;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class UsersController extends AbstractDao<Users> {

    public UsersController() {
        super(Users.class);
    }

    @Override
    public List<Users> findAll() {

        List<Users> lista = null;

        StringBuilder jpql = new StringBuilder();

        try {

            jpql.append(" SELECT user FROM Users AS user ");

            jpql.append(" WHERE 1=1 ");

            jpql.append(" ORDER BY user.id ");

            Query query = this.entityManager.createQuery(jpql.toString());

            lista = query.getResultList();

        } catch (Exception ex) {

            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);

        }

        return lista;
    }

    @Override
    public List<Users> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        return null;
    }

    @Override
    public Long count(Map<String, FilterMeta> filterBy) {
        return null;
    }
    
    public Users findByRut(String rut) {

        Users user = null;

        StringBuilder jpql = new StringBuilder();

        try {

            jpql.append(" SELECT user FROM Users AS user ");

            jpql.append(" LEFT JOIN FETCH user.cargos ");

            jpql.append(" WHERE 1=1 ");

            jpql.append(" AND user.rut = :rut ");

            Query query = this.entityManager.createQuery(jpql.toString());

            query.setParameter("rut", rut);

            query.setMaxResults(1);

            user = (Users) query.getSingleResult();

        } catch (Exception ex) {

            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);

        }

        return user;
    }
    
    public Users findById(int id) {

        Users user = null;

        StringBuilder jpql = new StringBuilder();

        try {

            jpql.append(" SELECT user FROM Users AS user ");

            jpql.append(" LEFT JOIN FETCH user.cargos ");

            jpql.append(" WHERE 1=1 ");

            jpql.append(" AND user.id = :id ");

            Query query = this.entityManager.createQuery(jpql.toString());

            query.setParameter("id", id);

            query.setMaxResults(1);

            user = (Users) query.getSingleResult();

        } catch (Exception ex) {

            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);

        }

        return user;
    }
    
    public Users findByUsername(Users u) {
        StringBuilder jpql = new StringBuilder();
        Users user = null;

        try {
            jpql.append("SELECT user FROM Users user ")
                    .append(" LEFT JOIN FETCH user.cargos ")
                    .append(" WHERE 1=1 ")
                    .append(" AND user.username = :username ");

            Query query = entityManager.createQuery(jpql.toString())
                    .setParameter("username", u.getUsername())
                    .setMaxResults(1);

            user = (Users) query.getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);
        }

        return user;
    }
}
