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


    /**
     * Inserta nuevo usuario a BBDD
     * @param clienteBanco Usuario a que queremos registrar
     * @return true si usuario ha sido insertado y false al contrario.
     */
    public boolean insertarUsuario(ClienteBanco clienteBanco) {
        if (clienteDAO.create(clienteBanco) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Recoge lista de todos numeros de cuentas que existen en BBDD
     *
     * @return Lista de Strings de numeros de todas las cuentas de base de datos.
     */
    public List<String> listaNumerosTodasCuentas() {
        return cuentaDAO.listaNumerosTodasCuentas();
    }


    /**
     * Inserta cuenta bancaria en base de datos
     *
     * @param c Objeto Cuenta que queremos insertar.
     * @return boolean true si ha sido insertada a BBDD y false al contrario.
     */
    public boolean insertarCuentaCliente(Cuenta c) {
        if (cuentaDAO.create(c) != null) {
            return true;
        }
        return false;
    }

    /**
     * Busca si existe usuario en base de datos segun su username.
     *
     * @param usuario Username de usuario que queremos buscar.
     * @return Objeto ClienteBanco, si no se encuentra, retorna null.
     */
    public ClienteBanco existeUsuario(String usuario) {
        return clienteDAO.existeUsuario(usuario);
    }

    /**
     * Busca si existe usuario en base de datos segun su DNI.
     *
     * @param dni DNI de usuario que queremos buscar.
     * @return Objeto ClienteBanco, si no se encuentra, retorna null.
     */
    public ClienteBanco existeDni(String dni) {
        return clienteDAO.existeDni(dni);
    }

    /**
     * Busca si existe usuario en base de datos segun su email.
     *
     * @param email Email de usuario que queremos buscar.
     * @return Objeto ClienteBanco, si no se encuentra, retorna null.
     */
    public ClienteBanco existeEmail(String email) {
        return clienteDAO.existeEmail(email);
    }

    /**
     * Valida nombre de cliente.
     *
     * @param nombre Nombre a ser validado.
     * @return true si el nombre es válido, false si no es válido.
     */
    public boolean validarNombre(String nombre) {
        Pattern pat = Pattern.compile("[A-Za-z]+");
        Matcher mat = pat.matcher(nombre);
        return mat.matches();
    }

    /**
     * Valida apellido de cliente.
     *
     * @param apellido Apellido a ser validado.
     * @return true si el apellido es válido, false si no es válido.
     */
    public boolean validarApellido(String apellido) {
        Pattern pat = Pattern.compile("[A-Za-z]+");
        Matcher mat = pat.matcher(apellido);
        return mat.matches();
    }

    /**
     * Valida DNI.
     *
     * @param dni El número de DNI a ser validado.
     * @return true si el DNI es válido, false si no es válido.
     */
    public boolean validarDNI(String dni) {
        Pattern pat = Pattern.compile("^[0-9]{8}[trwagmyfpdxbnjzsqvhlckeTRWAGMYFPDXBNJZSQVHLCKE]$");
        Matcher mat = pat.matcher(dni);
        return mat.matches();
    }

    /**
     * Valida edad de cliente.
     *
     * @param edad Edad a ser validado.
     * @return true si el edad es válido, false si no es válido.
     */
    public boolean validarEdad(String edad) {
        Pattern pat = Pattern.compile("[1-9][0-9]");
        Matcher mat = pat.matcher(edad);
        //necesita tener mas que 18 anos
        return mat.matches() && Integer.parseInt(edad) > 18;
    }

    /**
     * Valida email de cliente.
     *
     * @param email Email a ser validado.
     * @return true si el email es válido, false si no es válido.
     */
    public boolean validarEmail(String email) {
        Pattern pat = Pattern.compile("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$");
        Matcher mat = pat.matcher(email);
        return mat.matches();
    }

    /**
     * Valida username de cliente.
     *
     * @param username Username a ser validado.
     * @return true si el username es válido, false si no es válido.
     */
    public boolean validarUsuario(String username) {
        Pattern pat = Pattern.compile("[a-zA-Z0-9]{6}");
        Matcher mat = pat.matcher(username);
        return mat.matches();
    }

    /**
     * Valida contrasena de cliente.
     *
     * @param contrasena Contrasena a ser validada.
     * @return true si el contrasena es válida, false si no es válida.
     */
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

    /**
     * Valida codigo
     *
     * @param codigo Condigo que ha insertado usuario por terminal
     * @return true si el codigo es valido, false si no es válido.
     */
    public boolean validarCodigo(String codigo) {
        Pattern pat = Pattern.compile("[1-9]{4}");
        Matcher mat = pat.matcher(codigo);
        //necesita tener mas que 18 anos
        return mat.matches();
    }

    /**
     * Busca cuentas de usuario en BBDD
     *
     * @param usuarioActual Usuario de que cuentas queremos buscar
     * @return Lista de las cuentas de usuario
     */
    public List<Cuenta> buscarCuentasUsuario(ClienteBanco usuarioActual) {
        return cuentaDAO.buscarCuentasUsuario(usuarioActual);
    }


    /**
     * Recoge saldo actual de la cuenta del cliente
     *
     * @param numCuentaUsuario Numero de cuenta de que queremos ver el saldo actual
     * @return Saldo actual de la cuenta bancaria
     */
    public Double verSaldoDeCuenta(String numCuentaUsuario) {
        return cuentaDAO.verSaldoDeCuenta(numCuentaUsuario);
    }



}
