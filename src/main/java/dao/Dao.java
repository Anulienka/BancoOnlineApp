package dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import model.Cuenta;

import java.util.List;

abstract class Dao<T, K> {

    EntityManager em = Persistence
            .createEntityManagerFactory("default")
            .createEntityManager();

    public EntityManager getEntityManager() {
        return this.em;
    }

    public abstract T find(K id);

    public T create(T t) {
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();
        em.persist(t);
        try {
            entityTransaction.commit();
        } catch (Exception e) {
            System.out.println(e);
        }
        return t;
    }

    public T update(T t) {
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();
        em.merge(t);
        entityTransaction.commit();
        return t;
    }

    public void delete(T t) {
        EntityTransaction entityTransaction = em.getTransaction();
        entityTransaction.begin();
        em.remove(t);
        entityTransaction.commit();
    }


}
