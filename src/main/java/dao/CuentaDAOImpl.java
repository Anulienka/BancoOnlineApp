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

    @Override
    public Cuenta find(Integer id) {
        Cuenta cuenta = em.find(Cuenta.class, id);
        return cuenta;
    }

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

    /*@Override
    public Double verSaldoDeCuenta(String numCuentaUsuario) {
        Double saldoActual = 0.0;
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();


        try {
            String HQL_COD = "from Cuentas where numCuenta = :numCuentaUsuario";
            Query query = em.createQuery(HQL_COD);
            query.setParameter("numCuentaUsuario", numCuentaUsuario);

            saldoActual = (Double) query.getSingleResult();

            // Realiza el commit de la transacción
            entityTransaction.commit();
        } catch (NoResultException e) {
            // No se encontró ningún resultado
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
        return saldoActual;
    }*/

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
