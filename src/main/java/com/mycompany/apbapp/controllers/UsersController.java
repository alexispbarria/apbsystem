package com.mycompany.apbapp.controllers;

import com.mycompany.apbapp.dao.AbstractDao;
import com.mycompany.apbapp.entities.Cargos;
import com.mycompany.apbapp.entities.Users;
import com.mycompany.apbapp.exceptions.NegocioException;
import com.mycompany.apbapp.helpers.Util;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.mindrot.jbcrypt.BCrypt;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;

@Stateless
public class UsersController extends AbstractDao<Users> {

    @Inject
    private AuditLogController auditLogController;

    public UsersController() {
        super(Users.class);
    }

    @Override
    public List<Users> findAll() {
        List<Users> lista = new ArrayList<>();
        try {
            lista = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos ORDER BY u.id ASC",
                    Users.class
            ).getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findAll", ex);
        }
        return lista;
    }

    public List<Users> buscar(String texto, Integer cargoId, String estado) {
        List<Users> lista = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder();
            jpql.append("SELECT u FROM Users u LEFT JOIN FETCH u.cargos WHERE 1=1 ");

            boolean tieneTexto = (texto != null && !texto.isBlank());
            if (tieneTexto) {
                jpql.append(" AND (LOWER(u.fullName) LIKE :q OR LOWER(u.username) LIKE :q OR LOWER(u.rut) LIKE :q OR LOWER(u.email) LIKE :q) ");
            }
            if (cargoId != null && cargoId > 0) {
                jpql.append(" AND u.cargos.id = :cargoId ");
            }
            if ("ACTIVOS".equalsIgnoreCase(estado)) {
                jpql.append(" AND u.enabled = true ");
            } else if ("INACTIVOS".equalsIgnoreCase(estado)) {
                jpql.append(" AND u.enabled = false ");
            }
            jpql.append(" ORDER BY u.id ASC ");

            TypedQuery<Users> query = this.entityManager.createQuery(jpql.toString(), Users.class);
            if (tieneTexto) {
                query.setParameter("q", "%" + texto.trim().toLowerCase() + "%");
            }
            if (cargoId != null && cargoId > 0) {
                query.setParameter("cargoId", cargoId);
            }
            lista = query.getResultList();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "buscar", ex);
        }
        return lista;
    }

    @Override
    public List<Users> findAll(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        List<Users> lista = new ArrayList<>();
        try {
            TypedQuery<Users> q = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos ORDER BY u.id ASC",
                    Users.class
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
            return this.entityManager.createQuery("SELECT COUNT(u) FROM Users u", Long.class).getSingleResult();
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "count", ex);
            return 0L;
        }
    }

    public Users findByRut(String rut) {
        return findByRutSeguro(rut);
    }

    public Users findByRutSeguro(String rut) {
        if (rut == null || rut.isBlank()) {
            return null;
        }
        String norm = Util.formatearRUT(rut);
        if (norm == null) {
            return null;
        }
        try {
            List<Users> res = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos WHERE UPPER(u.rut) = :rut",
                    Users.class
            ).setParameter("rut", norm.toUpperCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByRutSeguro", ex);
            return null;
        }
    }

    public Users findById(int id) {
        try {
            List<Users> res = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos WHERE u.id = :id",
                    Users.class
            ).setParameter("id", id)
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findById", ex);
            return null;
        }
    }

    public Users findByUsernameTexto(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            List<Users> res = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos WHERE LOWER(u.username) = :username",
                    Users.class
            ).setParameter("username", username.trim().toLowerCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByUsernameTexto", ex);
            return null;
        }
    }

    public Users findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            List<Users> res = this.entityManager.createQuery(
                    "SELECT u FROM Users u LEFT JOIN FETCH u.cargos WHERE LOWER(u.email) = :email",
                    Users.class
            ).setParameter("email", email.trim().toLowerCase())
                    .setMaxResults(1)
                    .getResultList();
            return res.isEmpty() ? null : res.get(0);
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByEmail", ex);
            return null;
        }
    }

    public Users findByUsername(Users u) {
        if (u == null || u.getUsername() == null || u.getUsername().isBlank()) {
            return null;
        }
        Users user = null;
        try {
            Query query = entityManager.createQuery(
                    "SELECT user FROM Users user LEFT JOIN FETCH user.cargos WHERE LOWER(user.username) = :username"
            ).setParameter("username", u.getUsername().trim().toLowerCase())
                    .setMaxResults(1);

            List<?> res = query.getResultList();
            if (!res.isEmpty()) {
                user = (Users) res.get(0);
            }
        } catch (Exception ex) {
            this.rollBackOperation(this.getClass().getSimpleName(), "findByUsername", ex);
        }
        return user;
    }

    /**
     * Crea o edita una cuenta de usuario (exclusivo del Administrador).
     * Valida RUT (xxxxxxxx-x), unicidad, correo, cifra clave con BCrypt y registra auditoría.
     */
    public Users guardarUsuario(Users userForm,
                                Integer cargoId,
                                String rawPassword,
                                Users ejecutor,
                                String rolCodigo,
                                String ipOrigen) throws NegocioException {
        if (userForm == null) {
            throw new NegocioException("Datos de usuario inválidos.");
        }
        if (userForm.getFullName() == null || userForm.getFullName().isBlank()) {
            throw new NegocioException("El nombre completo es obligatorio.");
        }
        if (userForm.getUsername() == null || userForm.getUsername().isBlank()) {
            throw new NegocioException("El nombre de usuario es obligatorio.");
        }
        String usernameNorm = userForm.getUsername().trim().toLowerCase();
        if (!usernameNorm.matches("^[a-z0-9._-]{3,50}$")) {
            throw new NegocioException("El usuario debe tener entre 3 y 50 caracteres (solo letras minúsculas, números, punto, guion o guion bajo).");
        }
        if (userForm.getEmail() == null || !Util.validarEmail(userForm.getEmail().trim())) {
            throw new NegocioException("Ingresa un correo electrónico válido.");
        }
        String emailNorm = userForm.getEmail().trim().toLowerCase();

        String rutNorm = Util.formatearRUT(userForm.getRut());
        if (rutNorm == null || !Util.validarRut(rutNorm)) {
            throw new NegocioException("RUT inválido. Debe seguir la convención xxxxxxxx-x y tener dígito verificador correcto.");
        }

        if (cargoId == null) {
            throw new NegocioException("Debes asignar un cargo al usuario.");
        }
        Cargos cargo = this.entityManager.find(Cargos.class, cargoId);
        if (cargo == null) {
            throw new NegocioException("El cargo seleccionado no existe.");
        }

        // Validaciones de unicidad
        Users dupUser = findByUsernameTexto(usernameNorm);
        if (dupUser != null && (userForm.getId() == null || !dupUser.getId().equals(userForm.getId()))) {
            throw new NegocioException("El nombre de usuario '@" + usernameNorm + "' ya está en uso.");
        }
        Users dupRut = findByRutSeguro(rutNorm);
        if (dupRut != null && (userForm.getId() == null || !dupRut.getId().equals(userForm.getId()))) {
            throw new NegocioException("Ya existe una cuenta registrada con el RUT " + rutNorm + ".");
        }
        Users dupEmail = findByEmail(emailNorm);
        if (dupEmail != null && (userForm.getId() == null || !dupEmail.getId().equals(userForm.getId()))) {
            throw new NegocioException("El correo '" + emailNorm + "' ya se encuentra registrado.");
        }

        boolean esAdminPorCargo = "ADMIN".equalsIgnoreCase(cargo.getCodigo());
        Date now = new Date();
        boolean esNuevo = (userForm.getId() == null);
        Users guardado;

        if (esNuevo) {
            if (rawPassword == null || rawPassword.length() < 6) {
                throw new NegocioException("La contraseña inicial debe tener al menos 6 caracteres.");
            }
            Users nuevo = new Users();
            nuevo.setCreated(now);
            nuevo.setModified(now);
            nuevo.setEnabled(true);
            nuevo.setEsAdmin(esAdminPorCargo);
            nuevo.setUsername(usernameNorm);
            nuevo.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
            nuevo.setFullName(userForm.getFullName().trim());
            nuevo.setEmail(emailNorm);
            nuevo.setRut(rutNorm);
            nuevo.setCargos(cargo);

            this.entityManager.persist(nuevo);
            this.entityManager.flush();
            guardado = nuevo;
        } else {
            Users actual = this.entityManager.find(Users.class, userForm.getId());
            if (actual == null) {
                throw new NegocioException("El usuario a editar no existe.");
            }
            if (ejecutor != null && actual.getId().equals(ejecutor.getId())) {
                if (!userForm.getEnabled()) {
                    throw new NegocioException("No puedes desactivar tu propia cuenta en sesión.");
                }
                if (!esAdminPorCargo) {
                    throw new NegocioException("No puedes retirar tu propio cargo de Administrador.");
                }
            }

            actual.setFullName(userForm.getFullName().trim());
            actual.setUsername(usernameNorm);
            actual.setEmail(emailNorm);
            actual.setRut(rutNorm);
            actual.setCargos(cargo);
            actual.setEsAdmin(esAdminPorCargo);
            actual.setEnabled(userForm.getEnabled());
            actual.setModified(now);

            if (rawPassword != null && !rawPassword.isBlank()) {
                if (rawPassword.length() < 6) {
                    throw new NegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
                }
                actual.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
            }
            guardado = this.entityManager.merge(actual);
        }

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "USUARIOS",
                esNuevo ? "CREAR_USUARIO" : "EDITAR_USUARIO",
                "Users",
                guardado.getId(),
                (esNuevo ? "Creó cuenta @" : "Actualizó cuenta @") + guardado.getUsername()
                        + " (" + guardado.getFullName() + ", RUT: " + guardado.getRut()
                        + ", Cargo: " + cargo.getCodigo() + ")",
                ipOrigen
        );

        return guardado;
    }

    /**
     * Permite al Administrador restablecer la contraseña de un usuario (sin loguear la clave).
     */
    public void restablecerPassword(Integer userId,
                                    String nuevaPassword,
                                    Users ejecutor,
                                    String rolCodigo,
                                    String ipOrigen) throws NegocioException {
        if (userId == null) {
            throw new NegocioException("Usuario no especificado.");
        }
        if (nuevaPassword == null || nuevaPassword.length() < 6) {
            throw new NegocioException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        Users actual = this.entityManager.find(Users.class, userId);
        if (actual == null) {
            throw new NegocioException("Usuario no encontrado.");
        }

        actual.setPassword(BCrypt.hashpw(nuevaPassword, BCrypt.gensalt(10)));
        actual.setModified(new Date());
        this.entityManager.merge(actual);

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "USUARIOS",
                "RESTABLECER_PASSWORD",
                "Users",
                actual.getId(),
                "Restableció contraseña de la cuenta @" + actual.getUsername(),
                ipOrigen
        );
    }

    /**
     * Activa o desactiva una cuenta de usuario, impidiendo auto-desactivación.
     */
    public Users cambiarEstado(Integer userId,
                               boolean nuevoEstado,
                               Users ejecutor,
                               String rolCodigo,
                               String ipOrigen) throws NegocioException {
        if (userId == null) {
            throw new NegocioException("Usuario no especificado.");
        }
        if (ejecutor != null && userId.equals(ejecutor.getId()) && !nuevoEstado) {
            throw new NegocioException("No puedes desactivar tu propia cuenta mientras estás en sesión.");
        }

        Users actual = this.entityManager.find(Users.class, userId);
        if (actual == null) {
            throw new NegocioException("Usuario no encontrado.");
        }

        actual.setEnabled(nuevoEstado);
        actual.setModified(new Date());
        actual = this.entityManager.merge(actual);

        auditLogController.registrar(
                ejecutor,
                rolCodigo,
                "USUARIOS",
                nuevoEstado ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO",
                "Users",
                actual.getId(),
                (nuevoEstado ? "Activó cuenta @" : "Desactivó cuenta @") + actual.getUsername(),
                ipOrigen
        );

        return actual;
    }
}
