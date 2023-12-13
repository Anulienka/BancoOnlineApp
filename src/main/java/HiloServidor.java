import controlador.Controlador;
import model.ClienteBanco;
import model.Cuenta;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HiloServidor extends Thread {

    private Socket cliente;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String[] usuarioRegistrarInfo;
    private ClienteBanco usuarioActual;
    private int opcionMenu;
    private String[] credencialesUsuario;
    private String contrasenaHash;
    private Controlador controlador = new Controlador();
    private PublicKey publicaUsuario;
    private PrivateKey privada;
    private PublicKey publica;
    private Cuenta cuentaCliente;
    private List<String> numerosTodasCuentas;
    private List<Cuenta> cuentasCliente;
    private List<String> numerosDeCuentas;
    private byte[] numCuentaCifrada;
    private String numCuentaUsuario;

    public HiloServidor(Socket cliente){
        this.cliente = cliente;
    }

    @Override
    public void run() {

        try {
            ois = new ObjectInputStream(cliente.getInputStream());
            oos = new ObjectOutputStream(cliente.getOutputStream());
            generarClaves();
            //clave publica se manda al cliente
            System.out.println("Enviando clave publica al usuario");
            oos.writeObject(publica);
            System.out.println("Recibiendo clave publica del usuario");
            //Obtenemos la clave publica del cliente
            publicaUsuario = (PublicKey) ois.readObject();

        } catch (NoSuchAlgorithmException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        while (cliente.isConnected()) {

            try {
                //recogo cliente de banco para poder almacenar sus datos
                opcionMenu = (int) ois.readObject();
                switch (opcionMenu) {
                    case 1:
                        //recoge array con todos los datos de cliente
                        System.out.println("Recibiendo informacion de cliente");
                        //****** INFO CIFRADA ***
                        //descifrando informacion de cliente
                        //usuarioRegistrarInfo = recogerInfoUsuario();
                        //crea objeto ClienteBanco, para que se puede luego insertar a BBDD
                        //ClienteBanco usuarioBanco = crearUsuario(usuarioRegistrarInfo);

                        usuarioRegistrarInfo = (String[]) ois.readObject();
                        //crea objeto ClienteBanco, para que se puede luego insertar a BBDD
                        ClienteBanco usuarioBanco = crearUsuario(usuarioRegistrarInfo);

                        System.out.println("Creando documento para firma digital");
                        //si usuario firma es valida, usuario se registra
                        boolean verificada = enviarValidarNormasBancoFirmaDigital();
                        if (verificada) {
                            //registra cliente
                            System.out.println("Registrando usuario");
                            //comprueba si existe usuario con mismo email y username
                            if (controlador.existeUsuario(usuarioBanco.getEmail(), usuarioBanco.getUsuario()) == null) {
                                if (controlador.insertarUsuario(usuarioBanco)) {
                                    //registrado
                                    oos.writeUTF("R");
                                } else {
                                    //error
                                    oos.writeUTF("E");
                                }
                            } else {
                                //ya existe usuario con mismo Username
                                oos.writeUTF("EX");
                            }
                        } else {
                            oos.writeUTF("NS");
                        }
                        oos.flush();
                        break;

                    case 2:
                        //recoge credenciales desde parte cliente
                        System.out.println("Recibiendo credenciales del usuario");
                        credencialesUsuario = (String[]) ois.readObject();
                        //las valida
                        System.out.println("Hasheando contrasena que ha escrito usuario al iniciar sesion");
                        contrasenaHash = hashearContrasena(credencialesUsuario[1]);
                        //busca cliente segun usuario que ha insertado cuando queria inicial sesion
                        System.out.println("Buscando usuario");
                        usuarioActual = controlador.buscarUsuario(credencialesUsuario[0]);
                        if (usuarioActual != null) {
                            //comprueba si contrasenas Hash son iguales
                            System.out.println("Comprobando contrasena");
                            if (contrasenaHash.matches(usuarioActual.getContrasena())) {
                                System.out.println("Contrasena valida");
                                oos.writeUTF("V");
                            } else {
                                System.out.println("Contrasena no valida");
                                //no valido
                                oos.writeUTF("NV");
                            }
                        } else {
                            System.out.println("No existe usuario con Username insertado");
                            //no existe usuario con Username insertado
                            oos.writeUTF("NE");
                        }
                        oos.flush();

                        break;
                    case 3:
                        System.out.println("Creando cuenta bancaria para cliente");
                        String numeroCuenta = crearCuentaCliente();
                        Cuenta c = new Cuenta();
                        c.setNumCuenta(numeroCuenta);
                        c.setSaldo(0.0);
                        c.setClienteBanco(usuarioActual);
                        if (controlador.insertarCuentaCliente(c)) {
                            oos.writeUTF("Cuenta creada correctamente");
                        } else {
                            oos.writeUTF("Error en crear la cuenta");
                        }
                        oos.flush();
                        break;
                    case 4:
                        System.out.println("Buscando cuentas de cliente");
                        cuentasCliente = controlador.buscarCuentasUsuario(usuarioActual);
                        numerosDeCuentas = listarNumerosDeCuenta(cuentasCliente);
                        oos.writeObject(numerosDeCuentas);
                        //servidor recibe numero de cuenta de usuario cifrada
                        numCuentaCifrada = (byte[]) ois.readObject();
                        //luego la descifra
                        numCuentaUsuario = descifrar(numCuentaCifrada);
                        Double saldoDeCuenta = controlador.verSaldoDeCuenta(numCuentaUsuario);
                        //envia al cliente el saldo actual de la cuenta
                        oos.writeDouble(saldoDeCuenta);
                        oos.flush();
                        break;
                    case 5:
                        System.out.println("Haciendo transferencia");
                        cuentasCliente = controlador.buscarCuentasUsuario(usuarioActual);
                        numerosDeCuentas = listarNumerosDeCuenta(cuentasCliente);
                        oos.writeObject(numerosDeCuentas);
                        //servidor recibe numero de cuenta de usuario cifrada
                        numCuentaCifrada = (byte[]) ois.readObject();
                        //luego la descifra
                        numCuentaUsuario = descifrar(numCuentaCifrada);
                        //servidor recibe importe cifrado
                        byte[] importeCifrado = (byte[]) ois.readObject();
                        //luego lo descifra
                        String importe = descifrar(importeCifrado);
                        //el servidor se envia un código(cifrado)
                        enviarCodigo();
                        //recibe desde cliente si es valido el codigo
                        if(ois.readBoolean()){
                            //si es valido, hace importe

                            //ACABAR HACER IMPORTE


                            oos.writeUTF("Importe se ha hecho correctamente.");
                        }else {
                            oos.writeUTF("Importe ha sido rechasado.");
                        }
                        oos.flush();
                        break;
                }


            } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | SignatureException |
                     InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }
        cerrarFlujos(cliente, oos, ois);
    }

    private void enviarCodigo() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        Random random = new Random();
        //Genera un numero aleatorio de 4 dígitos (entre 1000 y 9999)
        int codigoAleatorio = random.nextInt(9000) + 1000;
        //cifro el codigo
        byte[] codigoCifrado = cifrar(String.valueOf(codigoAleatorio));
        //lo envio al cliente
        oos.writeObject(codigoCifrado);
    }

    private String[] recogerInfoUsuario() throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String[] usuarioInfo = new String[6];
        byte[] nombre = (byte[]) ois.readObject();
        String nombreUsuario = descifrar(nombre);
        byte[] apellido = (byte[]) ois.readObject();
        String apellidoUsuario = descifrar(apellido);
        byte[] edad = (byte[]) ois.readObject();
        String edadUsuario = descifrar(edad);
        byte[] email = (byte[]) ois.readObject();
        String emailUsuario = descifrar(email);
        byte[] usuario = (byte[]) ois.readObject();
        String username = descifrar(usuario);
        byte[] contrasena = (byte[]) ois.readObject();
        String contrasenaUsuario = descifrar(contrasena);
        usuarioInfo = new String[]{nombreUsuario, apellidoUsuario, edadUsuario, emailUsuario, username, contrasenaUsuario};
        return usuarioInfo;
    }


    private List<String> listarNumerosDeCuenta(List<Cuenta> cuentasCliente) {
        List<String> cuentas = new ArrayList<>();
        for (Cuenta c : cuentasCliente) {
            cuentas.add(c.getNumCuenta());
        }
        return cuentas;
    }

    /**
     * Descifra el dato utilizando la clave privada del servidor con RSA algoritmo.
     *
     * @param datoParaDescifrar Datos cifrados que se van a descifrar.
     * @return Los datos descifrados como una cadena de caracteres.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de relleno especificado.
     * @throws InvalidKeyException       Si se produce un error con la clave proporcionada.
     * @throws IllegalBlockSizeException Si se produce un error con el tamaño del bloque.
     * @throws BadPaddingException       Si se produce un error con el relleno de los datos.
     */
    private String descifrar(byte[] datoParaDescifrar) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher descipher = Cipher.getInstance("RSA");
        descipher.init(Cipher.DECRYPT_MODE, privada);
        String datoDescifrado = new String(descipher.doFinal(datoParaDescifrar));
        return datoDescifrado;
    }

    /**
     * Cifra una cadena de caracteres utilizando la clave publica del usuario con RSA algoritmo.
     *
     * @param datoParaCifrar La cadena de caracteres que se va a cifrar.
     * @return Los datos cifrados como un array de bytes.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de relleno especificado.
     * @throws InvalidKeyException       Si se produce un error con la clave proporcionada.
     * @throws IllegalBlockSizeException Si se produce un error con el tamaño del bloque.
     * @throws BadPaddingException       Si se produce un error con el relleno de los datos.
     */
    private byte[] cifrar(String datoParaCifrar) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicaUsuario);
        //directamente cifrarlo en un array de bytes, y no hacer conversiones a string
        byte[] datoCifrado = cipher.doFinal(datoParaCifrar.getBytes());
        return datoCifrado;
    }


    /**
     * Genera un par de claves para servidor con algoritmo RSA (pública y privada).
     *
     * @throws NoSuchAlgorithmException Si no se encuentra el algoritmo de generación de claves especificado.
     */
    private void generarClaves() throws NoSuchAlgorithmException {
        //Primero se intercambian las claves publicas entre usuario y servidor
        //crea clave publica y privada para cada cliente
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        KeyPair par = keygen.generateKeyPair();
        privada = par.getPrivate();
        publica = par.getPublic();
    }

    private boolean enviarValidarNormasBancoFirmaDigital() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ClassNotFoundException {
        boolean check = false;
        //FIRMA DIGITAL - documento firmado digitalmente por el servidor
        String normasBanco = "Normas de banco";
        //envia normas de banco al cliente para firmar
        oos.writeObject(normasBanco);
        //recibe normas de banco de cliente
        String normasBancoCliente = (String) ois.readObject();
        //verifica la firma
        Signature verificarFirma = Signature.getInstance("SHA1WITHRSA");
        verificarFirma.initVerify(publicaUsuario);
        //que queremos verificar
        verificarFirma.update(normasBancoCliente.getBytes());

        //verificando
        byte[] firma= (byte[]) ois.readObject();

        if(firma != null){
            check = verificarFirma.verify(firma);
        }
        return check;
    }

    private ClienteBanco crearUsuario(String[] usuarioRegistrarInfo) throws NoSuchAlgorithmException {
        System.out.println("Hasheando contrasena");
        contrasenaHash = hashearContrasena(this.usuarioRegistrarInfo[5]);
        ClienteBanco clienteBanco = new ClienteBanco();
        clienteBanco.setNombre(this.usuarioRegistrarInfo[0]);
        clienteBanco.setApellido(this.usuarioRegistrarInfo[1]);
        clienteBanco.setEdad(Integer.parseInt(this.usuarioRegistrarInfo[2]));
        clienteBanco.setEmail(this.usuarioRegistrarInfo[3]);
        clienteBanco.setUsuario(this.usuarioRegistrarInfo[4]);
        clienteBanco.setContrasena(contrasenaHash);
        //retorna objeto clienteBanco
        System.out.println("Se ha creado objeto cliente");
        return clienteBanco;
    }

    /**
     * Hashea la contraseña utilizando el algoritmo SHA-256 y devuelve el resultado en formato hexadecimal.
     *
     * @param contrasena La contraseña que queremos hashear.
     * @return La representación en formato hexadecimal del hash de la contraseña.
     * @throws NoSuchAlgorithmException Si no se encuentra el algoritmo de hash especificado.
     */
    public String hashearContrasena(String contrasena) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        byte[] contrasenaByte = contrasena.getBytes();
        md.update(contrasenaByte);
        //esto es contrasena HASH
        byte[] resumen = md.digest();

        //para que devuelve como string
        StringBuilder contrasenaHash = new StringBuilder();
        for (byte b : resumen) {
            contrasenaHash.append(String.format("%02x", b));
        }
        System.out.println("Contrasena se ha hasheado");
        return contrasenaHash.toString();
    }


    /**
     * Crea y devuelve un número de cuenta bancaria para un cliente, asegurándose que numero de la cuenta es unico.
     *
     * @return El número de cuenta bancaria creado para el cliente.
     */
    private String crearCuentaCliente() {
        //lista de todas las cuentas
        numerosTodasCuentas = controlador.listaNumerosTodasCuentas();
        //cuenta bancaria tiene formato ESOO00000000000000000000
        StringBuilder numeroCuenta = new StringBuilder("ES");
        Random random = new Random();

        //se repite hasta que se crea un numero de cuenta que todavia no existe en BBDD
        do {
            for (int i = 0; i < 22; i++) {
                int digito = random.nextInt(10);
                numeroCuenta.append(digito);
            }


        } while (numerosTodasCuentas.contains(numeroCuenta.toString()));

        return numeroCuenta.toString();
    }


    /**
     * Cierra los flujos de comunicación y el socket asociado.
     *
     * @param cliente El socket que se va a cerrar.
     * @param oos     El flujo de salida de objetos que se va a cerrar.
     * @param ois     El flujo de entrada de objetos que se va a cerrar.
     */
    private void cerrarFlujos(Socket cliente, ObjectOutputStream oos, ObjectInputStream ois) {
        try {
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
            if (cliente != null) {
                cliente.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
