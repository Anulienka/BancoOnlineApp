package dao;

import idao.ClienteDAO;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import model.ClienteBanco;
import model.Cuenta;

public class ClienteDAOImpl extends Dao<ClienteBanco, Integer> implements ClienteDAO {

    /**
     * Busca y recupera un cliente del banco por su id.
     *
     * @param id El id del cliente a buscar.
     * @return El objeto ClienteBanco asociado al ID proporcionado, o null si no se encuentra ningún cliente.
     */
    @Override
    public ClienteBanco find(Integer id) {
        ClienteBanco cliente = em.find(ClienteBanco.class, id);
        return cliente;
    }

    /**
     * Verifica si existe un cliente en la base de datos con el usuario proporcionado.
     *
     * @param usuario El nombre de usuario del cliente a verificar.
     * @return El objeto ClienteBanco asociado al nombre de usuario proporcionado, o null si no se encuentra ningún cliente.
     */
    @Override
    public ClienteBanco existeUsuario(String usuario) {
        ClienteBanco clienteBanco = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Clientes where usuario = :usuario";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("usuario", usuario);

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

    /**
     * Verifica si existe un cliente en la base de datos con el dni proporcionado.
     *
     * @param dni El DNI del cliente a verificar.
     * @return El objeto ClienteBanco asociado DNI proporcionado, o null si no se encuentra ningún cliente.
     */
    @Override
    public ClienteBanco existeDni(String dni) {
        ClienteBanco clienteBanco = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Clientes where dni = :dni";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("dni", dni);

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
