package idao;

import model.ClienteBanco;

/**
 * Interfaz que define las operaciones disponibles para acceder y manipular información de clientes en la base de datos.
 */
public interface ClienteDAO {

    ClienteBanco existeUsuario(String usuario);
    ClienteBanco existeDni(String dni);
}
