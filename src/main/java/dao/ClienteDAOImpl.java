package dao;

import idao.ClienteDAO;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import model.ClienteBanco;
import model.Cuenta;

public class ClienteDAOImpl extends Dao<ClienteBanco, Integer> implements ClienteDAO {

    @Override
    public ClienteBanco find(Integer id) {
        ClienteBanco cliente = em.find(ClienteBanco.class, id);
        return cliente;
    }


    @Override
    public ClienteBanco existeUsuario(String usuario) {
        ClienteBanco clienteBanco = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Clientes where usuario = :usuario";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("usuario", usuario);


            // Obtiene el pieza
            clienteBanco = (ClienteBanco) query.getSingleResult();

            // Realiza el commit de la transacción
            entityTransaction.commit();
        } catch (NoResultException e) {
            // No se encontró ningún resultado
            clienteBanco = null;
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }
        return clienteBanco;
    }

    @Override
    public ClienteBanco existeDni(String dni) {
        ClienteBanco clienteBanco = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Clientes where dni = :dni";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("dni", dni);


            // Obtiene el pieza
            clienteBanco = (ClienteBanco) query.getSingleResult();

            // Realiza el commit de la transacción
            entityTransaction.commit();
        } catch (NoResultException e) {
            // No se encontró ningún resultado
            clienteBanco = null;
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }
        return clienteBanco;    }

    public ClienteBanco existeEmail(String email) {
        ClienteBanco clienteBanco = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Clientes where email = :email";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("email", email);


            // Obtiene el pieza
            clienteBanco = (ClienteBanco) query.getSingleResult();

            // Realiza el commit de la transacción
            entityTransaction.commit();
        } catch (NoResultException e) {
            // No se encontró ningún resultado
            clienteBanco = null;
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }
        return clienteBanco;    }
}
