package idao;

import model.ClienteBanco;

public interface ClienteDAO {

    ClienteBanco buscarCliente(String usuario);
    ClienteBanco existeUsuario(String email, String usuario);
}
