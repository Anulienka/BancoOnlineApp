package idao;

import model.ClienteBanco;

public interface ClienteDAO {

    ClienteBanco existeUsuario(String usuario);
    ClienteBanco existeDni(String dni);
    ClienteBanco existeEmail(String email);

}
