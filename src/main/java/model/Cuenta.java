package model;

import jakarta.persistence.*;

import java.io.Serializable;


@Entity (name = "Cuentas")
public class Cuenta{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column(name = "numCuenta", nullable = false)
    private String numCuenta;
    @Column(name = "saldo")
    private double saldo;

    @ManyToOne (optional = false, cascade = { CascadeType.MERGE, CascadeType.REFRESH })
    @JoinColumn(name = "cliente_id", referencedColumnName = "id")
    private ClienteBanco clienteBanco;


    public Cuenta() {
    }

    public Cuenta(String numCuenta) {
        this.numCuenta = numCuenta;
    }

    public String getNumCuenta() {
        return numCuenta;
    }

    public void setNumCuenta(String numCuenta) {
        this.numCuenta = numCuenta;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public Integer getId() {
        return id;
    }

    public ClienteBanco getClienteBanco() {
        return clienteBanco;
    }

    public void setClienteBanco(ClienteBanco clienteBanco) {
        this.clienteBanco = clienteBanco;
    }

}
