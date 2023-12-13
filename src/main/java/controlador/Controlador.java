package controlador;

import dao.ClienteDAOImpl;
import dao.CuentaDAOImpl;
import model.ClienteBanco;
import model.Cuenta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controlador {

    private ClienteDAOImpl clienteDAO = new ClienteDAOImpl();
    private CuentaDAOImpl cuentaDAO = new CuentaDAOImpl();


    public boolean insertarUsuario(ClienteBanco clienteBanco) {
        if (clienteDAO.create(clienteBanco) != null) {
            return true;
        } else {
            return false;
        }
    }


    public List<String> listaNumerosTodasCuentas() {
        return cuentaDAO.listaNumerosTodasCuentas();
    }


    public boolean insertarCuentaCliente(Cuenta c) {
        if (cuentaDAO.create(c) != null) {
            return true;
        }
        return false;
    }


    public ClienteBanco buscarUsuario(String usuario) {
        return clienteDAO.buscarCliente(usuario);
    }

    public ClienteBanco existeUsuario(String email, String usuario) {
        return clienteDAO.existeUsuario(email, usuario);
    }


    public boolean validarNombre(String nombre) {
        Pattern pat = Pattern.compile("[A-Za-z]+");
        Matcher mat = pat.matcher(nombre);
        return mat.matches();
    }

    public boolean validarApellido(String apellido) {
        Pattern pat = Pattern.compile("[A-Za-z]+");
        Matcher mat = pat.matcher(apellido);
        return mat.matches();
    }

    public boolean validarEdad(String edad) {
        Pattern pat = Pattern.compile("[1-9][0-9]");
        Matcher mat = pat.matcher(edad);
        //necesita tener mas que 18 anos
        return mat.matches() && Integer.parseInt(edad) > 18;
    }

    public boolean validarEmail(String email) {
        Pattern pat = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$");
        Matcher mat = pat.matcher(email);
        return mat.matches();
    }

    public boolean validarUsuario(String usuario) {
        Pattern pat = Pattern.compile("[a-zA-Z0-9]{6}");
        Matcher mat = pat.matcher(usuario);
        return mat.matches();
    }

    public boolean validarContrasena(String contrasena) {
        /*^ represents starting character of the string.
        (?=.*[0-9]) por lo menos un numero.
        (?=.*[a-z]) por lo menos una minuscula.
        (?=.*[A-Z]) por lo menos una mayuscula.
        (?=\\S+$) no se permiten espacios.
        .{10} contrasena deberia tener longitud 10.
        $ final de String.*/

        Pattern pat = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{10}$");
        Matcher mat = pat.matcher(contrasena);
        return mat.matches();
    }

    public boolean validarCodigo(String codigo) {
        Pattern pat = Pattern.compile("[1-9]{4}");
        Matcher mat = pat.matcher(codigo);
        //necesita tener mas que 18 anos
        return mat.matches();
    }

    public List<Cuenta> buscarCuentasUsuario(ClienteBanco usuarioActual) {
        return cuentaDAO.buscarCuentasUsuario(usuarioActual);
    }

    public Double verSaldoDeCuenta(String numCuentaUsuario) {
        return cuentaDAO.verSaldoDeCuenta(numCuentaUsuario);
    }
}
