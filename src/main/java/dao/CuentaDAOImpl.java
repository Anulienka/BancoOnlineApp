package dao;

import idao.CuentaDAO;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import model.ClienteBanco;
import model.Cuenta;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAOImpl extends Dao<Cuenta,Integer> implements CuentaDAO {

    /**
     * Busca una cuenta en la base de datos utilizando su id.
     *
     * @param id El id de la cuenta a buscar.
     * @return La cuenta asociada al id, o null si no se encuentra ninguna cuenta.
     */
    @Override
    public Cuenta find(Integer id) {
        Cuenta cuenta = em.find(Cuenta.class, id);
        return cuenta;
    }

    /**
     * Obtiene una lista de todos los números de cuentas existentes en la base de datos.
     *
     * @return Una lista que contiene los números de todas las cuentas en la base de datos.
     */
    @Override
    public List<String> listaNumerosTodasCuentas() {
        List<String> numCuentas = new ArrayList<>();
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "SELECT numCuenta FROM Cuentas";
            Query query = em.createQuery(HQL_COD);

            numCuentas = query.getResultList();

            // Realiza el commit de la transacción
            entityTransaction.commit();

        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }

        return numCuentas;
    }

    /**
     * Busca y devuelve una lista de cuentas asociadas al cliente.
     *
     * @param usuarioActual El cliente para el cual se buscan las cuentas.
     * @return Una lista de cuentas asociadas al cliente proporcionado.
     */
    @Override
    public List<Cuenta> buscarCuentasUsuario(ClienteBanco usuarioActual) {
        List<Cuenta> cuentasUsuario = new ArrayList<>();
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "FROM Cuentas c WHERE c.clienteBanco.id = :idUsuario";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("idUsuario", usuarioActual.getId());

            cuentasUsuario = query.getResultList();

            // Realiza el commit de la transacción
            entityTransaction.commit();

        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }

        return cuentasUsuario;
    }

    /**
     * Busca y devuelve la cuenta asociada al número de cuenta especificado.
     *
     * @param numeroCuenta El número de cuenta de la cuenta que se desea buscar.
     * @return La cuenta asociada al número de cuenta proporcionado o null si no se encuentra.
     */
    @Override
    public Cuenta buscarCuenta(String numeroCuenta) {
        Cuenta cuenta = null;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "FROM Cuentas c WHERE c.numCuenta = :numeroCuenta";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("numeroCuenta", numeroCuenta);

            cuenta = (Cuenta) query.getSingleResult();

            // Realiza el commit de la transacción
            entityTransaction.commit();

        } catch (Exception e) {
            // Maneja las excepciones, realiza rollback si es necesario
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            e.printStackTrace();
        }

        return cuenta;
    }
}
