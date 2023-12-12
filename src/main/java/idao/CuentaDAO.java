package idao;

import model.ClienteBanco;
import model.Cuenta;

import java.util.List;

public interface CuentaDAO {

    public abstract List<String> listaNumerosTodasCuentas();

    List<Cuenta> buscarCuentasUsuario(ClienteBanco usuarioActual);

    Double verSaldoDeCuenta(String numCuentaUsuario);
}
