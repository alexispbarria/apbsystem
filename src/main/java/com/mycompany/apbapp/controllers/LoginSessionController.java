package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.LoginSessions;
import com.mycompany.apbapp.entities.Users;
import jakarta.ejb.Stateless;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class LoginSessionController extends AbstractDao<LoginSessions> {


    public LoginSessionController() {
        super(LoginSessions.class);
    }

    @Override
    public List<LoginSessions> findAll() {

        List<LoginSessions> lista = new ArrayList<>();

        StringBuilder jpql = new StringBuilder();

        try {

            jpql.append(" SELECT login FROM LoginSessions AS olgin ");

            jpql.append(" WHERE 1=1 ");

            jpql.append(" AND TIMESTAMPDIFF(MINUTE, login.created, NOW()) >= 180 ");

            Query query = this.entityManager.createQuery(jpql.toString());

            lista = query.getResultList();

        } catch (Exception ex) {

            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);

        }

        return lista;
    }

    @Override
    public boolean create(LoginSessions loginSession) {

        loginSession.setCreated(new Date());
        loginSession.setModified(new Date());

        return super.create(loginSession);
    }

    public LoginSessions getBySessionUUID(String session_uuid) {

        LoginSessions login = null;

        StringBuilder jpql = new StringBuilder();

        try {

            jpql.append(" SELECT loginSession FROM LoginSessions AS loginSession ");

            jpql.append(" LEFT JOIN FETCH loginSession.users ");

            jpql.append(" WHERE 1=1 ");

            jpql.append(" AND loginSession.enabled = true ");

            jpql.append(" AND loginSession.sessionUuid = :session_uuid ");

            jpql.append(" AND MOD(HOUR(TIMEDIFF( NOW(), loginSession.created )), 24) < 3 ");

            Query query = this.entityManager.createQuery(jpql.toString());

            query.setMaxResults(1);

            query.setParameter("session_uuid", session_uuid);

            login = (LoginSessions) query.getSingleResult();

        } catch (Exception ex) {
            System.out.println(ex);

            this.rollBackOperation(this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName(), ex);

        }

        return login;
    }

    @Override
    public List<LoginSessions> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Long count(Map<String, FilterMeta> filters) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

//    public String loginFalso(Users users, String sessionUuid) throws Exception {
//
//        if (users != null) {
//            LoginSessions session = new LoginSessions();
//
//            session.setUsers(users);
//            session.setAppName("https://normalizacion.gobiernosantiago.cl");
//            session.setCodigo("APPSYSTEM");
//            session.setCreated(new Date());
//            session.setEnabled(true);
//            session.setModified(new Date());
//            session.setSessionUuid(sessionUuid);
//
//            if (super.create(session)) {
//                return sessionUuid;
//            }
//
//        }
//
//        return "";
//    }

    public boolean registrarLogin(String app_name, String uuid, Users user) {

        LoginSessions login = new LoginSessions();

        login.setAppName(app_name);
        login.setSessionUuid(uuid);
        login.setUsers(user);
        login.setEnabled(true);

        login.setCodigo("SYSTEMAPP");

        return this.create(login);
    }

//    public boolean createAutoLogin(HttpSession session, String url) {
//        String url_name = url;
//
//        LoginSessions login = new LoginSessions();
//        login.setAppName(url_name);
//        login.setSessionUuid((String) session.getAttribute("session_uuid"));
//        login.setSgaUsers((SgaUsers) session.getAttribute("user"));
//        login.setCreated(new Date());
//        login.setModified(new Date());
//        login.setEnabled(true);
//        login.setCodigo("DOCUMENTAL");
//
//        if (this.create(login)) {
//            return sgaLoginSessionHistoryController.createHistory(login);
//        }
//
//        return false;
//    }
}
