package idao;

import model.ClienteBanco;
import model.Cuenta;

import java.util.List;

/**
 * Interfaz que define las operaciones disponibles para acceder y manipular información de cuentas en la base de datos.
 */
public interface CuentaDAO {

    List<String> listaNumerosTodasCuentas();

    //List<Cuenta> buscarCuentasUsuario(ClienteBanco usuarioActual);

    Double verSaldoDeCuenta(String numCuentaUsuario);

    Cuenta buscarCuenta(String numeroCuenta);
}
