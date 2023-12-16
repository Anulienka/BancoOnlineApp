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

/**
 * @author Anna
 * <p>
 * La clase HiloServidor representa un hilo que gestiona conexiones de clientes del banco.
 * <p>
 * Recibe informacion que escribe usuario en consola y le envia a cliente la informacion y mensajes
 * Dependiendo que opcion elije cliente, hace operaciones en aplicacion
 */
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
    private List<byte[]> numerosDeCuentas;
    private byte[] numCuentaCifrada;
    private String numCuentaUsuario;

    /**
     * Constructor de la clase HiloServidor.
     *
     * @param cliente El socket de conexión del cliente.
     */
    public HiloServidor(Socket cliente) {
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
                //recojo cliente de banco para poder almacenar sus datos
                opcionMenu = (int) ois.readObject();

                switch (opcionMenu) {
                    case 1:
                        System.out.println("Recibiendo informacion de cliente");
                        //recoge informacion del cliente
                        usuarioRegistrarInfo = recogerInfoUsuario();
                        //crea objeto ClienteBanco, para que se puede luego insertar a BBDD
                        ClienteBanco usuarioBanco = crearUsuario(usuarioRegistrarInfo);
                        System.out.println("Creando documento para firma digital");
                        //si usuario firma es valida, usuario se registra
                        boolean verificada = enviarValidarNormasBancoFirmaDigital();
                        if (verificada) {
                            //registra cliente
                            System.out.println("Registrando usuario " + usuarioBanco.getUsuario());
                            //comprueba si existe usuario con mismo DNI, username y contrasena
                            if (controlador.insertarUsuario(usuarioBanco)) {
                                //registrado
                                oos.writeUTF("R");
                            } else {
                                //error
                                oos.writeUTF("E");
                            }
                        } else {
                            oos.writeUTF("NS");
                        }
                        oos.flush();
                        break;

                    case 2:
                        //recoge credenciales desde parte cliente
                        credencialesUsuario = recogerCredencialesUsuario();
                        System.out.println("Recibiendo credenciales del usuario " + credencialesUsuario[0]);
                        //busca cliente segun usuario que ha insertado cuando queria inicial sesion
                        System.out.println("Buscando usuario " + credencialesUsuario[0]);
                        usuarioActual = controlador.existeUsuario(credencialesUsuario[0]);
                        if (usuarioActual != null) {
                            //comprueba si contrasenas Hash son iguales
                            System.out.println("Comprobando contrasena");
                            if (credencialesUsuario[1].matches(usuarioActual.getContrasena())) {
                                System.out.println("Contrasena valida");
                                oos.writeUTF("V");
                            } else {
                                System.out.println("Contrasena no valida");
                                //no valido
                                oos.writeUTF("NV");
                            }
                        } else {
                            System.out.println("No existe clinete con usuario insertado.");
                            //no existe usuario con Username insertado
                            oos.writeUTF("NE");
                        }
                        oos.flush();

                        break;
                    case 3:
                        System.out.println("Creando cuenta bancaria para cliente " + usuarioActual.getUsuario());
                        String numeroCuenta = crearCuentaCliente();
                        Cuenta c = new Cuenta();
                        c.setNumCuenta(numeroCuenta);
                        c.setSaldo(0.0);
                        c.setClienteBanco(usuarioActual);
                        boolean cuentaCreada = controlador.insertarCuentaCliente(c);
                        if (cuentaCreada) {
                            System.out.println("Cuenta creada");
                            oos.writeObject(cifrar(c.getNumCuenta()));
                        } else {
                            System.out.println("Error en crear la cuenta");
                            oos.writeObject(null);
                        }
                        oos.flush();
                        break;
                    case 4:
                        System.out.println("Buscando cuentas de cliente " + usuarioActual.getUsuario());
                        cuentasCliente = controlador.buscarCuentasUsuario(usuarioActual);
                        numerosDeCuentas = listarNumerosDeCuenta(cuentasCliente);
                        oos.writeObject(numerosDeCuentas);
                        //servidor recibe numero de cuenta de usuario cifrada
                        numCuentaCifrada = (byte[]) ois.readObject();
                        if (numCuentaCifrada != null) {
                            //luego la descifra
                            System.out.println("Recibiendo cuenta del cliente.");
                            numCuentaUsuario = descifrar(numCuentaCifrada);
                            cuentaCliente = controlador.buscarCuenta(numCuentaUsuario);
                            //envia al cliente el saldo actual de la cuenta
                            System.out.println("Enviando saldo actual al cliente");
                            oos.writeDouble(cuentaCliente.getSaldo());
                            oos.flush();
                        } else {
                            oos.writeDouble(0.0);
                            oos.flush();
                        }

                        break;
                    case 5:
                        System.out.println("Haciendo transferencia para cliente " + usuarioActual.getUsuario());
                        cuentasCliente = controlador.buscarCuentasUsuario(usuarioActual);
                        numerosDeCuentas = listarNumerosDeCuenta(cuentasCliente);
                        oos.writeObject(numerosDeCuentas);
                        //servidor recibe numero de cuenta de usuario cifrada
                        numCuentaCifrada = (byte[]) ois.readObject();
                        if (numCuentaCifrada != null) {
                            //luego la descifra
                            numCuentaUsuario = descifrar(numCuentaCifrada);
                            //que operacion de banco ha elegido cliente
                            String operacion = ois.readUTF();
                            if (operacion.equals("I")) {
                                //-->Ingresar dinero a la cuenta
                                System.out.println("Haciendo ingreso");
                                //servidor recibe importe cifrado
                                byte[] importeCifrado = (byte[]) ois.readObject();
                                //luego lo descifra
                                String importe = descifrar(importeCifrado);
                                //el servidor se envia un código(cifrado)
                                System.out.println("Enviando codigo de confirmacion");
                                enviarCodigo();
                                //recibe desde cliente si es valido el codigo
                                if (ois.readBoolean()) {
                                    cuentaCliente = controlador.buscarCuenta(numCuentaUsuario);
                                    cuentaCliente.setSaldo(cuentaCliente.getSaldo() + Double.parseDouble(importe));
                                    if (controlador.modificarCuenta(cuentaCliente)) {
                                        oos.writeUTF("Ingreso se ha hecho correctamente.");
                                    } else {
                                        oos.writeUTF("Error.");
                                    }
                                } else {
                                    oos.writeUTF("Has excedido el número máximo de intentos. El ingreso no se ha realizado.");
                                }
                                oos.flush();

                            } else {
                                //-->Gastar dinero de la cuenta
                                System.out.println("Haciendo retirada");
                                //servidor recibe importe cifrado
                                byte[] importeCifrado = (byte[]) ois.readObject();
                                //luego lo descifra
                                String importe = descifrar(importeCifrado);
                                //COMPROBAR SI HAY SALDO SUFICIENTE EN LA CUENTA
                                cuentaCliente = controlador.buscarCuenta(numCuentaUsuario);
                                boolean haySaldoSufuciente = (cuentaCliente.getSaldo() - Double.parseDouble(importe)) > 0;
                                oos.writeBoolean(haySaldoSufuciente);
                                if (haySaldoSufuciente) {
                                    //el servidor se envia un código(cifrado)
                                    System.out.println("Enviando codigo de confirmacion");
                                    enviarCodigo();
                                    //recibe desde cliente si es valido el codigo
                                    if (ois.readBoolean()) {
                                        //si es valido, hace importe
                                        cuentaCliente.setSaldo(cuentaCliente.getSaldo() - Double.parseDouble(importe));
                                        if (controlador.modificarCuenta(cuentaCliente)) {
                                            oos.writeUTF("Retirada se ha hecho correctamente.");
                                        } else {
                                            oos.writeUTF("Error.");
                                        }
                                    } else {
                                        oos.writeUTF("Has excedido el número máximo de intentos. La retirada no se ha realizado.");
                                    }
                                    oos.flush();
                                } else {
                                    oos.writeUTF("No hay saldo suficiente para hacer retirada.");
                                }
                                oos.flush();
                            }
                        }
                        break;
                }

            } catch (ClassNotFoundException | NoSuchAlgorithmException | SignatureException |
                     InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (IOException ioException){
                System.out.println("Cliente se ha desconectado");
                break;
            }
        }

        cerrarFlujos(cliente, oos, ois);

    }

    private String[] recogerCredencialesUsuario() throws IOException, ClassNotFoundException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String[] credenciales = new String[2];

        byte[] username = (byte[]) ois.readObject();
        String usernameUsuario = descifrar(username);
        credenciales[0] = usernameUsuario;

        byte[] contrasena = (byte[]) ois.readObject();
        credenciales[1] = hashearContrasena(descifrar(contrasena));

        return credenciales;
    }

    /**
     * Genera un código aleatorio de 4 dígitos, lo cifra y lo envía al cliente.
     *
     * @throws NoSuchPaddingException    Si no se encuentra el algoritmo de relleno especificado.
     * @throws IllegalBlockSizeException Si se produce un error durante el cifrado debido a un tamaño de bloque no válido.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws BadPaddingException       Si se produce un error durante el cifrado debido a un relleno incorrecto.
     * @throws InvalidKeyException       Si se produce un error con la clave de cifrado.
     * @throws IOException               Si ocurre un error durante la escritura del código cifrado al cliente.
     */
    private void enviarCodigo() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, IOException {
        Random random = new Random();
        //Genera un numero aleatorio de 4 dígitos (entre 1000 y 9999)
        int codigoAleatorio = random.nextInt(9000) + 1000;
        //cifro el codigo
        byte[] codigoCifrado = cifrar(String.valueOf(codigoAleatorio));
        //lo envio al cliente
        oos.writeObject(codigoCifrado);
    }

    /**
     * Recoge la información del usuario (nombre, apellido,DNI, edad, email, usuario y contraseña) cifrada, la descifra la almacena en array de Strings
     * Valida la existencia del DNI y del nombre de usuario en la BBDD y confirma al usuario si existe cliente con mismo DNI o usuario
     *
     * @return Un array de Strings con la información del usuario.
     * [nombre, apellido, DNI, edad, email, usuario, contraseña]
     * @throws IOException               Si hay un error en la operación de entrada/salida.
     * @throws ClassNotFoundException    Si la clase del objeto recibido no se encuentra.
     * @throws NoSuchPaddingException    Si el tipo de relleno utilizado en el cifrado no está disponible.
     * @throws NoSuchAlgorithmException  Si el algoritmo de cifrado utilizado no está disponible.
     * @throws InvalidKeyException       Si se utiliza una clave no válida para inicializar el cifrado.
     * @throws IllegalBlockSizeException Si se produce un error en el tamaño del bloque en el cifrado.
     * @throws BadPaddingException       Si se produce un error en el relleno en el cifrado.
     */
    private String[] recogerInfoUsuario() throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String[] usuarioInfo = new String[7];

        byte[] nombre = (byte[]) ois.readObject();
        usuarioInfo[0] = descifrar(nombre);

        byte[] apellido = (byte[]) ois.readObject();
        usuarioInfo[1] = descifrar(apellido);

        while (true) {
            byte[] dni = (byte[]) ois.readObject();
            String dniUsuario = descifrar(dni);
            if (controlador.existeDni(dniUsuario) == null) {
                usuarioInfo[2] = dniUsuario;
                oos.writeBoolean(true);
                oos.flush();
                break;
            } else {
                oos.writeBoolean(false);
                oos.flush();
            }
        }

        byte[] edad = (byte[]) ois.readObject();
        usuarioInfo[3] = descifrar(edad);

        byte[] email = (byte[]) ois.readObject();
        usuarioInfo[4] = descifrar(email);

        while (true) {
            byte[] username = (byte[]) ois.readObject();
            String usernameUsuario = descifrar(username);
            if (controlador.existeUsuario(usernameUsuario) == null) {
                usuarioInfo[5] = usernameUsuario;
                oos.writeBoolean(true);
                oos.flush();
                break;
            } else {
                oos.writeBoolean(false);
                oos.flush();
            }
        }

        byte[] contrasena = (byte[]) ois.readObject();
        usuarioInfo[6] = hashearContrasena(descifrar(contrasena));

        return usuarioInfo;
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
        return contrasenaHash.toString();
    }

    /**
     * Obtiene una lista de números de cuenta a partir de una lista de cuentas.
     *
     * @param cuentasCliente La lista de cuentas del cliente.
     * @return Una lista de números de cuents de cliente.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de hash especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de padding especificado.
     * @throws IllegalBlockSizeException Si el tamaño del bloque es ilegal para el algoritmo de cifrado.
     * @throws BadPaddingException       Si ocurre un error en el padding durante la operación de descifrado.
     * @throws InvalidKeyException       Si se utiliza una clave inválida para el algoritmo de cifrado.
     */
    private List<byte[]> listarNumerosDeCuenta(List<Cuenta> cuentasCliente) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<byte[]> cuentas = new ArrayList<>();
        if (cuentasCliente != null) {
            for (Cuenta c : cuentasCliente) {
                cuentas.add(cifrar(c.getNumCuenta()));
            }
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

    /**
     * Envía las normas del banco al cliente para su firma digital y verifica la firma recibida del cliente.
     *
     * @return true si las normas del banco fueron firmadas y verificadas correctament, false en caso contrario.
     * @throws IOException              Si ocurre un error durante la comunicación con el cliente.
     * @throws NoSuchAlgorithmException Si no se encuentra el algoritmo de firma especificado.
     * @throws InvalidKeyException      Si ocurre un error con la clave de firma.
     * @throws SignatureException       Si ocurre un error durante la verificación de la firma.
     * @throws ClassNotFoundException   Si no se puede cargar la clase de objeto al recibir la firma.
     */
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
        byte[] firma = (byte[]) ois.readObject();

        if (firma != null) {
            check = verificarFirma.verify(firma);
        }
        return check;
    }

    /**
     * Crea objeto ClienteBanco con sus atributos que recoge de array de Strings
     *
     * @param usuarioRegistrarInfo El array de Strings con informacion de cliente(nombre, apellido, DNI, edad, email, usuario, contrasena).
     * @return objeto ClienteBanco, que queremos registrar
     */
    private ClienteBanco crearUsuario(String[] usuarioRegistrarInfo) throws NoSuchAlgorithmException {
        ClienteBanco clienteBanco = new ClienteBanco();
        clienteBanco.setNombre(this.usuarioRegistrarInfo[0]);
        clienteBanco.setApellido(this.usuarioRegistrarInfo[1]);
        clienteBanco.setDni(this.usuarioRegistrarInfo[2]);
        clienteBanco.setEdad(Integer.parseInt(this.usuarioRegistrarInfo[3]));
        clienteBanco.setEmail(this.usuarioRegistrarInfo[4]);
        clienteBanco.setUsuario(this.usuarioRegistrarInfo[5]);
        clienteBanco.setContrasena(this.usuarioRegistrarInfo[6]);
        //retorna objeto clienteBanco
        System.out.println("Se ha creado objeto cliente");
        return clienteBanco;
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
