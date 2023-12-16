import controlador.Controlador;
import model.ClienteBanco;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anna
 * <p>
 * La clase Cliente representa un cliente en la aplicación de banco
 * Se conecta al servidor y permite a usuario comunicar con aplicacion de banco(servidor)
 */
public class Cliente {

    final int PUERTO = 4444;
    private Socket cliente;
    private Controlador controlador = new Controlador();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private String mensajeBanco;
    private PublicKey publicaServidor;
    private PrivateKey privada;
    private PublicKey publica;
    private List<byte[]> numerosCuentasUsuario;
    private String cuentaUsuario;
    private byte[] numCuentaCifrada;

    public static void main(String[] args) throws IOException {
        Cliente c = new Cliente();
        c.initClient();
    }

    private void initClient() throws IOException {

        cliente = new Socket("localhost", PUERTO);

        ObjectOutputStream oos = new ObjectOutputStream(cliente.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
        int opcionMenu = -1;

        //Primero se intercambian las claves publicas entre usuario y servidor
        try {
            //obtenemos la clave publica de servidor
            publicaServidor = (PublicKey) ois.readObject();
            //generamos las claves del usuario
            generarClaves();
            //mandamos la clave del cliente al servidor
            oos.writeObject(publica);

        } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
            System.out.println(e);
        }

        while (cliente.isConnected()) {

            try {
                System.out.println("****** BIENVENIDO A BANCO ESTRELLA FINANCIERA (BEF) ******");
                System.out.println("1. Registrarse");
                System.out.println("2. Iniciar sesion");

                opcionMenu = Integer.parseInt(br.readLine());

                switch (opcionMenu) {
                    case 1:
                        while (true) {
                            System.out.println("\n****** FORMULARIO DE REGISTRACION DE NUEVO CLIENTE ******");
                            oos.writeObject(1);
                            registrarCliente(oos, ois);
                            //FIRMA DIGITAL
                            firmarDocumento(ois, oos);
                            //recibe mensaje del banco
                            mensajeBanco = ois.readUTF();
                            if (mensajeBanco.equals("R")) {
                                System.out.println("¡Usuario ha sido registrado con éxito y puede usar la aplicación del banco!\n");
                                break;
                            } else if (mensajeBanco.equals("E")) {
                                System.out.println("Error en registrar.");
                            } else if (mensajeBanco.equals("NS")) {
                                System.out.println("No se han aceptado normas de banco, cliente no ha sido registrado.\n");
                            }
                        }

                    case 2:
                        while (true) {
                            System.out.println("\n****** INICIAR SESION ******");
                            System.out.println("Para poder realizar operaciones bancarias, \nse necesita iniciar sesion insertando usuario y contraseña.\n");
                            oos.writeObject(2);
                            String nombreCliente = iniciarSesion(oos);
                            //informacion que llega desde servidor
                            String valido = ois.readUTF();
                            if (valido.equals("V")) {
                                int opcionMenuBanco = -1;
                                //entra en opcion de banco
                                System.out.println("\n****** BIENVENIDO CLIENTE " + nombreCliente + " ******");
                                while (true) {

                                    try {
                                        System.out.println("\nElige opcion:");
                                        System.out.println("1. Crear cuenta bancaria");
                                        System.out.println("2. Ver saldo de cuenta bancaria");
                                        System.out.println("3. Operaciones de cuenta bancaria");

                                        opcionMenuBanco = Integer.parseInt(br.readLine());

                                        switch (opcionMenuBanco) {
                                            case 1:
                                                //envio a servidor que quiero crear una cuenta nueva
                                                oos.writeObject(3);
                                                byte[] cuentaNumero = (byte[]) ois.readObject();
                                                if (cuentaNumero != null) {
                                                    System.out.println("Tu cuenta bancaria ha sido creada con exito.");
                                                    System.out.println("Numero de la cuenta nueva: " + descifrar(cuentaNumero));
                                                } else {
                                                    System.out.println("Error en crear la cuenta. Intenta otra vez!.");
                                                }
                                                break;
                                            case 2:
                                                oos.writeObject(4);
                                                numerosCuentasUsuario = (List<byte[]>) ois.readObject();
                                                cuentaUsuario = elegirCuentaUsuario(numerosCuentasUsuario);
                                                if (cuentaUsuario.equals("")) {
                                                    oos.writeObject(null);
                                                    double noHayCuenta = ois.readDouble();
                                                } else {
                                                    numCuentaCifrada = cifrar(cuentaUsuario);
                                                    oos.writeObject(numCuentaCifrada);
                                                    System.out.println("Saldo actual de la cuenta: " + ois.readDouble() + " €\n");
                                                }

                                                break;
                                            case 3:
                                                oos.writeObject(5);
                                                //usuario elige cuenta de cual quiere hacer transferencia
                                                numerosCuentasUsuario = (List<byte[]>) ois.readObject();
                                                cuentaUsuario = elegirCuentaUsuario(numerosCuentasUsuario);
                                                if (cuentaUsuario.equals("")) {
                                                    oos.writeObject(null);
                                                } else {
                                                    numCuentaCifrada = cifrar(cuentaUsuario);
                                                    //primero envio numero de cuenta cifrada
                                                    oos.writeObject(numCuentaCifrada);
                                                    //usuario elige si quiere hacer ingreso o retirada
                                                    elegirOperacionBanco(oos, ois);
                                                }
                                                break;
                                        }

                                    } catch (NumberFormatException | IOException | NoSuchPaddingException |
                                             IllegalBlockSizeException | BadPaddingException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            } else if (valido.equals("NV")) {
                                System.out.println("La contraseña proporcionada es incorrecta. Por favor, inténtalo de nuevo.\n");
                            } else {
                                System.out.println("No existe cliente registrado con el usuario proporcionado.\n");
                            }
                        }
                }


            } catch (NumberFormatException | IOException | ClassNotFoundException | NoSuchAlgorithmException |
                     SignatureException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                     BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }

        cerrarFlujos(cliente, oos, ois);
    }


    /**
     * Registra un nuevo cliente en la aplicacion de banco, pidiendo información necesaria
     * como nombre, apellido, DNI, edad, email, usuario y contraseña. Se valida cada campo
     * antes de cifrarlo y enviarlo al servidor para asegurar datos correctos.
     *
     * @param oos ObjectOutputStream para enviar datos al servidor.
     * @param ois ObjectInputStream para recibir datos del servidor.
     * @throws IOException               Si ocurre un error durante la operación de entrada/salida.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de hash especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de padding especificado.
     * @throws IllegalBlockSizeException Si el tamaño del bloque es ilegal para el algoritmo de cifrado.
     * @throws BadPaddingException       Si ocurre un error en el padding durante la operación de descifrado.
     * @throws InvalidKeyException       Si se utiliza una clave inválida para el algoritmo de cifrado.
     */
    private void registrarCliente(ObjectOutputStream oos, ObjectInputStream ois) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        String dni;
        String email;
        String contrasena;
        String nombre;
        String usuario;
        String apellido;
        String edad = null;

        do {
            System.out.print("Nombre: ");
            nombre = br.readLine();
            if (controlador.validarNombre(nombre)) {
                oos.writeObject(cifrar(nombre.substring(0, 1).toUpperCase() + nombre.substring(1)));
                oos.flush();
                break;
            }
        } while (true);


        do {
            System.out.print("Apellido: ");
            apellido = br.readLine();
            if (controlador.validarApellido(apellido)) {
                oos.writeObject(cifrar(apellido.substring(0, 1).toUpperCase() + apellido.substring(1)));
                oos.flush();
                break;
            }
        } while (true);


        while (true) {
            System.out.print("DNI: ");
            dni = br.readLine();
            if (controlador.validarDNI(dni)) {
                oos.writeObject(cifrar(dni.substring(0, 8) + dni.substring(8).toUpperCase()));
                oos.flush();
                boolean dniNoExiste = ois.readBoolean();
                if (dniNoExiste) {
                    break;
                } else {
                    System.out.println("Cliente con mismo DNI ya existe.");
                }
            }
        }

        do {
            System.out.print("Edad: ");
            edad = br.readLine();
            if (controlador.validarEdad(edad)) {
                oos.writeObject(cifrar(edad));
                oos.flush();
                break;
            }
        } while (true);


        do {
            System.out.print("Email: ");
            email = br.readLine();
            if (controlador.validarEmail(email)) {
                oos.writeObject(cifrar(email));
                oos.flush();
                break;
            }
        } while (true);


        while (true) {
            System.out.println("Usuario \n(debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
            if (controlador.validarUsuario(usuario)) {
                oos.writeObject(cifrar(usuario));
                oos.flush();
                boolean usernameNoExiste = ois.readBoolean();
                if (usernameNoExiste) {
                    break;
                } else {
                    System.out.println("Cliente con mismo usuario ya existe.");
                }
            }
        }

        while (true) {
            System.out.println("Contraseña: ");
            System.out.println("(debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            if (controlador.validarContrasena(contrasena)) {
                oos.writeObject(cifrar(contrasena));
                oos.flush();
                break;
            }
        }
    }

    /**
     * Recibe un código cifrado enviado por el servidor, lo descifre y pide al usuario que lo inserte correctamente,
     * y luego envia al servidor true si ha codigo es valido o false si el usuario no ha escrito 3 veces mismo codigo que ha enviado servidor.
     *
     * @param ois ObjectInputStream, flujo de entrada para recibir datos del servidor.
     * @param oos ObjectOutputStream, flujo de salida para enviar datos al servidor.
     * @throws IOException               Si ocurre un error durante la comunicación con el servidor.
     * @throws ClassNotFoundException    Si no se puede cargar la clase de objeto al recibir datos del servidor.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el algoritmo de relleno especificado.
     * @throws InvalidKeyException       Si se produce un error con la clave de cifrado.
     * @throws IllegalBlockSizeException Si se produce un error durante el cifrado debido a un tamaño de bloque no válido.
     * @throws BadPaddingException       Si se produce un error durante el cifrado debido a un relleno incorrecto.
     */
    private void insertarCodigoTransaccion(ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // codigo cifrado desde servidor, que el usuario debe leerlo, descifrarlo e insertarlo correctamente.
        byte[] codigo = (byte[]) ois.readObject();
        //descifra el codigo
        String codigoServidor = descifrar(codigo);
        System.out.println("\n" + codigoServidor);
        boolean codigoValido = insertarComprobarCodigo(codigoServidor);
        //envia al servidor si es valido o no el codigo
        oos.writeBoolean(codigoValido);
        oos.flush();
    }

    /**
     * Permite al usuario elegir entre realizar un ingreso o una retirada en la cuenta bancaria.
     * Se pide de usuario que inserta importe, se cifra, envia al servidor y se recoge informacion del servidor si transaccion ha sido realizada o no
     *
     * @param oos ObjectOutputStream, flujo de salida para enviar datos al servidor.
     * @param ois ObjectInputStream, flujo de entrada para recibir datos del servidor.
     * @throws IOException               Si ocurre un error durante la comunicación con el servidor.
     * @throws NoSuchPaddingException    Si no se encuentra el algoritmo de relleno especificado.
     * @throws IllegalBlockSizeException Si se produce un error durante el cifrado debido a un tamaño de bloque no válido.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws BadPaddingException       Si se produce un error durante el cifrado debido a un relleno incorrecto.
     * @throws InvalidKeyException       Si se produce un error con la clave de cifrado.
     * @throws ClassNotFoundException    Si no se puede cargar la clase de objeto al recibir datos del servidor.
     */
    private void elegirOperacionBanco(ObjectOutputStream oos, ObjectInputStream ois) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, ClassNotFoundException {
        char opcion = 'O';

        while (true) {
            System.out.println("\nIngreso(I) / Retirada(R)");
            opcion = br.readLine().toUpperCase().charAt(0);

            if (opcion == 'I') {
                oos.writeUTF("I");
                oos.flush();
                //luego envio importe cifrado
                double importe = insertarImporte();
                byte[] importeCifrado = cifrar(String.valueOf(importe));
                oos.writeObject(importeCifrado);
                insertarCodigoTransaccion(ois, oos);
                //recibe mensaje del servidor, si se ha hecho transaccion o no
                System.out.println(ois.readUTF());
                break;
            } else if (opcion == 'R') {
                oos.writeUTF("R");
                oos.flush();
                double importe = insertarImporte();
                byte[] importeCifrado = cifrar(String.valueOf(importe));
                oos.writeObject(importeCifrado);
                boolean saldoSuficiente = ois.readBoolean();
                if (saldoSuficiente) {
                    insertarCodigoTransaccion(ois, oos);
                    System.out.println(ois.readUTF());
                } else {
                    System.out.println(ois.readUTF());
                    break;
                }
                break;
            }

        }
    }

    /**
     * Genera un par de claves para cliente con algoritmo RSA (pública y privada).
     *
     * @throws NoSuchAlgorithmException Si no se encuentra el algoritmo de generación de claves especificado.
     */
    private void generarClaves() throws NoSuchAlgorithmException {
        KeyPairGenerator clavesUsuario = KeyPairGenerator.getInstance("RSA");
        KeyPair par = clavesUsuario.generateKeyPair();
        privada = par.getPrivate();
        publica = par.getPublic();
    }

    /**
     * Pide el usuario que inserte el codigo de 4 digitos y comprueba si el código ingresado coincide con codigo que ha enviaro servidor.
     * El usuario tiene 3 intentos para escribir el código correcto
     *
     * @param codigoServidor El código proporcionado por el servidor para comparación.
     * @return true si el código ingresado coincide con el código del servidor, false si codigos con diferentes.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura.
     */
    private boolean insertarComprobarCodigo(String codigoServidor) throws IOException {
        boolean codigoValido = false;
        String codigo = "";
        int contador = 0;

        System.out.println("Inserta codigo de la pantalla:");
        while (codigoValido == false) {
            //usuario puede insertar codigo 3 veces
            if (contador <= 2) {
                codigo = br.readLine();
                //comprueba si tiene 4 numeros
                boolean formatoValidoCodigo = controlador.validarCodigo(codigo);
                if (formatoValidoCodigo && codigoServidor.equals(codigo)) {
                    codigoValido = true;
                } else {
                    System.out.println("Codigo incorrecto! Intenta otra vez!");
                    contador++;
                }
            } else {
                break;
            }
        }
        return codigoValido;
    }

    /**
     * Descifra el dato utilizando la clave privada de cliente con RSA algoritmo.
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
     * Metodo para firmar documento de normas de banco utilizando firma digital
     *
     * @param ois El flujo de entrada que lee datos enviados desde servidor (documento a firmar).
     * @param oos El flujo de salida que envia datos al servidor (firma digital y documetno).
     * @throws IOException              Si ocurre un error de entrada/salida durante la lectura o escritura.
     * @throws ClassNotFoundException   Si no se encuentra la clase del objeto leído.
     * @throws NoSuchAlgorithmException Si no se encuentra el algoritmo de firma digital especificado.
     * @throws InvalidKeyException      Si se produce un error con la clave de firma digital.
     * @throws SignatureException       Si se produce un error durante la firma del documento.
     **/
    private void firmarDocumento(ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //recibe documento desde servidor
        String normasBanco = ois.readObject().toString();
        System.out.println("\n" + normasBanco);
        char opcion = 'O';

        while (true) {
            System.out.println("\nAcceptas normas de banco? (S/N)");
            opcion = br.readLine().toUpperCase().charAt(0);

            if (opcion == 'S') {
                //lo firma
                Signature signature = Signature.getInstance("SHA1WITHRSA");
                signature.initSign(privada);
                signature.update(normasBanco.getBytes());
                oos.writeObject(normasBanco);
                byte[] firma = signature.sign();
                oos.writeObject(firma);
                break;
            } else if (opcion == 'N') {
                oos.writeObject(normasBanco);
                oos.writeObject(null);
                break;
            }
            oos.flush();
        }
    }

    /**
     * Cifra una cadena de caracteres utilizando la clave publica del servidor con RSA algoritmo.
     *
     * @param datoParaCifrar La cadena de caracteres que se va a cifrar.
     * @return Los datos cifrados como un array de bytes.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de cifrado especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de relleno especificado.
     * @throws InvalidKeyException       Si se produce un error con la clave proporcionada.
     * @throws IllegalBlockSizeException Si se produce un error con el tamaño del bloque.
     * @throws BadPaddingException       Si se produce un error con el relleno de los datos.
     */
    private byte[] cifrar(String datoParaCifrar) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicaServidor);
        //directamente cifrarlo en un array de bytes, y no hacer conversiones a string
        byte[] datoCifrado = cipher.doFinal(datoParaCifrar.getBytes());
        return datoCifrado;
    }

    /**
     * Metodo para elegir la cuenta del usuario.
     *
     * @param cuentasUsuario Lista de numeros de cuentas del usuario cifrados en formato byte[].
     * @return Numero de cuenta elegida en forma de cadena de caracteres.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de hash especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de padding especificado.
     * @throws IllegalBlockSizeException Si el tamaño del bloque es ilegal para el algoritmo de cifrado.
     * @throws BadPaddingException       Si ocurre un error en el padding durante la operación de descifrado.
     * @throws InvalidKeyException       Si se utiliza una clave inválida para el algoritmo de cifrado.
     */
    private String elegirCuentaUsuario(List<byte[]> cuentasUsuario) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        List<String> cuentasDescifradas = new ArrayList<>();
        int contador = 1;
        boolean opcionValida = false;
        int opcionCuenta = 0;
        String numCuenta = "";

        if (cuentasUsuario.isEmpty()) {
            System.out.println("Usuario no tiene cuenta asignada. Hay que crear cuenta primero.");
        } else {
            System.out.println("");
            for (byte[] c : cuentasUsuario) {
                String numeroCuenta = descifrar(c);
                cuentasDescifradas.add(numeroCuenta);
                System.out.println(contador + ". " + numeroCuenta);
                contador++;
            }

            while (!opcionValida) {
                try {
                    System.out.println("Elije cuenta bancaria:");
                    opcionCuenta = Integer.parseInt(br.readLine());
                    if (opcionCuenta < 1 || opcionCuenta > cuentasUsuario.size()) {
                        System.out.println("Opcion inválida.");
                    } else {
                        //retorna empleado elegido
                        numCuenta = cuentasDescifradas.get(opcionCuenta - 1);
                        opcionValida = true;
                    }
                } catch (Exception e) {
                    System.out.println("Opcion inválida. Intenta otra vez insertando numero.");
                }
            }
        }

        return numCuenta;
    }

    /**
     * Metodo para insertar importe de la cuenta de usuario.
     *
     * @return Importe que cuiere hacer en usuario en su cuenta en fromaro double.
     */
    private double insertarImporte() {
        boolean formatoValido = false;
        double importe = 0.0;

        while (!formatoValido) {
            try {
                System.out.println("Inserta importe: ");
                importe = Double.parseDouble(br.readLine());
                formatoValido = true;
            } catch (Exception e) {
                System.out.println("Opcion inválida. Intenta otra vez insertando numero.");
            }
        }
        return importe;
    }

    /**
     * Recoge las credenciales del usuario, las valida, las cifra y envia al servidor
     *
     * @return String con Usuario de cliente
     * @throws IOException               Si ocurre un error de entrada/salida durante la lectura.
     * @throws IOException               Si ocurre un error durante la operación de entrada/salida.
     * @throws NoSuchAlgorithmException  Si no se encuentra el algoritmo de hash especificado.
     * @throws NoSuchPaddingException    Si no se encuentra el esquema de padding especificado.
     * @throws IllegalBlockSizeException Si el tamaño del bloque es ilegal para el algoritmo de cifrado.
     * @throws BadPaddingException       Si ocurre un error en el padding durante la operación de descifrado.
     * @throws InvalidKeyException       Si se utiliza una clave inválida para el algoritmo de cifrado.
     */
    private String iniciarSesion(ObjectOutputStream oos) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        String contrasena;
        String usuario;

        while (true) {
            System.out.println("USUARIO (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
            if (controlador.validarUsuario(usuario)) {
                byte[] usuarioCifrado = cifrar(usuario);
                oos.writeObject(usuarioCifrado);
                oos.flush();
                break;
            }
        }

        while (true) {
            System.out.println("CONTRASEÑA: ");
            System.out.println("(debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            if (controlador.validarContrasena(contrasena)) {
                byte[] contrasenaCifrada = cifrar(contrasena);
                oos.writeObject(contrasenaCifrada);
                oos.flush();
                break;
            }
        }

        return usuario;
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