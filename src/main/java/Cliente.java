import controlador.Controlador;
import model.ClienteBanco;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.*;
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
    private List<String> numerosCuentasUsuario;
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
        String[] clienteBancoInfo;
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
            throw new RuntimeException(e);
        }

        while (cliente.isConnected()) {

            try {
                System.out.println("****** BIENVENIDO A BANCO ******");
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
                            String[] credenciales = iniciarSesion();
                            //envio al servidor usuario y contrasena de cliente para que lo valida
                            oos.writeObject(credenciales);
                            //informacion que llega desde servidor
                            String valido = ois.readUTF();
                            if (valido.equals("V")) {
                                int opcionMenuBanco = -1;
                                //entra en opcion de banco
                                System.out.println("\n****** BIENVENIDO CLIENTE " + credenciales[0].toUpperCase() + " ******");
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
                                                mensajeBanco = ois.readUTF();
                                                System.out.println(mensajeBanco);
                                                break;
                                            case 2:
                                                oos.writeObject(4);
                                                numerosCuentasUsuario = (List<String>) ois.readObject();
                                                cuentaUsuario = elegirCuentaUsuario(numerosCuentasUsuario);
                                                if(cuentaUsuario.equals("")){
                                                  oos.writeObject(null);
                                                  double noHayCuenta = ois.readDouble();
                                                }else{
                                                    numCuentaCifrada = cifrar(cuentaUsuario);
                                                    oos.writeObject(numCuentaCifrada);
                                                    System.out.println("Saldo actual de la cuenta: " + ois.readDouble() + "\n");
                                                }

                                                break;
                                            case 3:
                                                oos.writeObject(5);
                                                //usuario elige cuenta de cual quiere hacer transferencia
                                                numerosCuentasUsuario = (List<String>) ois.readObject();
                                                cuentaUsuario = elegirCuentaUsuario(numerosCuentasUsuario);
                                                if(cuentaUsuario.equals("")){
                                                    oos.writeObject(null);
                                                }else{
                                                    numCuentaCifrada = cifrar(cuentaUsuario);
                                                    //primero envio numero de cuenta cifrada
                                                    oos.writeObject(numCuentaCifrada);
                                                    //usuario envia elige si quiere hacer ingreso o gasto
                                                    elegirOperacionBanco(oos, ois);
                                                    //insertarCodigoTransaccion(ois, oos);
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
                     SignatureException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }

        cerrarFlujos(cliente, oos, ois);
    }

    private void registrarCliente(ObjectOutputStream oos, ObjectInputStream ois) throws IOException, NoSuchAlgorithmException {
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
            if(controlador.validarNombre(nombre.substring(0, 1).toUpperCase() + nombre.substring(1))){
                nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
                oos.writeUTF(nombre);
                oos.flush();
                break;
            }
        } while (true);


        do {
            System.out.print("Apellido: ");
            apellido = br.readLine();
            if(controlador.validarApellido(apellido.substring(0, 1).toUpperCase() + apellido.substring(1))){
                apellido = apellido.substring(0, 1).toUpperCase() + apellido.substring(1);
                oos.writeUTF(apellido);
                oos.flush();
                break;
            }
        } while (true);


        while (true){
            System.out.print("DNI: ");
            dni = br.readLine();
            if (controlador.validarDNI(dni)){
                oos.writeUTF(dni.substring(0, 8) + dni.substring(8).toUpperCase());
                oos.flush();
                boolean dniNoExiste = ois.readBoolean();
                if(dniNoExiste){
                    break;
                }else{
                    System.out.println("Cliente con mismo DNI ya existe");
                }
            }
        }

        do {
            System.out.print("Edad: ");
            edad = br.readLine();
            if(controlador.validarEdad(edad)){
                oos.writeUTF(edad);
                oos.flush();
                break;
            }
        } while (true);


        do {
            System.out.print("Email: ");
            email = br.readLine();
            if(controlador.validarEmail(email)){
                oos.writeUTF(email);
                oos.flush();
                break;
            }
        } while (true);


        while (true){
            System.out.print("Usuario (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
            if (controlador.validarUsuario(usuario)){
                oos.writeUTF(usuario);
                oos.flush();
                boolean usernameNoExiste = ois.readBoolean();
                if(usernameNoExiste){
                    break;
                }else{
                    System.out.println("Cliente con mismo usuario ya existe.");
                }
            }
        }

        do {
            System.out.println("Contraseña: ");
            System.out.println("(debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            if(controlador.validarContrasena(contrasena)){
                String contrasenaHasheada = hashearContrasena(contrasena);
                oos.writeUTF(contrasenaHasheada);
                oos.flush();
                break;
            }
        } while (true);
    }

    /**
     * Recibe un código cifrado enviado por el servidor, lo descifre y pide al usuario que lo inserte correctamente,
     * y luego envia al servidor true si ha codigo es valido o false si el usuario no ha metido 3 veces mismo codigo que ha enviado servidor.
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
        System.out.println(codigoServidor);
        boolean codigoValido = insertarComprobarCodigo(codigoServidor);
        //envia al servidor si es valido o no el codigo
        oos.writeBoolean(codigoValido);
        oos.flush();
    }

    /**
     * Permite al usuario elegir entre realizar un ingreso o un gasto en la cuenta bancaria.
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
            System.out.println("\nIngreso(I) / Gasto(G)");
            opcion = br.readLine().toUpperCase().charAt(0);

            if (opcion == 'I') {
                oos.writeUTF("I");
                oos.flush();
                //luego envio inporte cifrado
                double importe = insertarImporte();
                byte[] importeCifrado = cifrar(String.valueOf(importe));
                oos.writeObject(importeCifrado);
                insertarCodigoTransaccion(ois, oos);
                //recibe mensaje del servidor, si se ha hecho transaccion o no
                System.out.println(ois.readUTF());
                break;
            } else if (opcion == 'G') {
                oos.writeUTF("G");
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
     * @param cuentasUsuario Lista de numeros de cuenta de todas las cuentas del usuario en formato String.
     * @return Numero de cuenta elegida en forma de cadena de caracteres.
     */
    private String elegirCuentaUsuario(List<String> cuentasUsuario) {
        int contador = 1;
        boolean opcionValida = false;
        int opcionCuenta = 0;
        String numCuenta = "";

        if(cuentasUsuario.isEmpty()){
            System.out.println("Usuario no tiene cuenta asignada. Hay que crear cuenta primero.");
        }else{
            System.out.println("");
            for (String c : cuentasUsuario) {
                System.out.println(contador + ". " + c);
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
                        numCuenta = cuentasUsuario.get(opcionCuenta - 1);
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
     * Metodo para iniciar sesión de usuario solicitando y validando el nombre de usuario y la contraseña.
     *
     * @return Un array que contiene el nombre de usuario en la posición 0 y la contraseña en la posición 1.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura.
     */
    private String[] iniciarSesion() throws IOException, NoSuchAlgorithmException {
        String contrasena;
        String usuario;
        String contrasenaHasheada;
        String[] credenciales = new String[2];

        do {
            System.out.println("USUARIO (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
        } while (!controlador.validarUsuario(usuario));

        do {
            System.out.println("CONTRASEÑA: ");
            System.out.println("(debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            contrasenaHasheada = hashearContrasena(contrasena);
        } while (!controlador.validarContrasena(contrasena));

        credenciales[0] = usuario;
        credenciales[1] = contrasenaHasheada;

        return credenciales;
    }

    /**
     * Metodo para registrar a un nuevo cliente en el banco solicitando y validando su información personal.
     *
     * @return Un array que contiene la información del cliente, en el orden: Nombre, Apellido, DNI, Edad, Email, Usuario, Contraseña.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura.
     */
    private String[] registrarClienteBanco() throws IOException, NoSuchAlgorithmException {
        String[] infoCliente = new String[7];

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
            infoCliente[0] = nombre.substring(0, 1).toUpperCase() + nombre.substring(1);
        } while (!controlador.validarNombre(nombre));

        do {
            System.out.print("Apellido: ");
            apellido = br.readLine();
            infoCliente[1] = apellido.substring(0, 1).toUpperCase() + apellido.substring(1);
        } while (!controlador.validarApellido(apellido));

        do {
            System.out.print("DNI: ");
            dni = br.readLine();
            infoCliente[2] = dni.substring(0, 8) + dni.substring(8).toUpperCase();
        } while (!controlador.validarDNI(dni));

        do {
            System.out.print("Edad: ");
            try {
                edad = br.readLine();
                infoCliente[3] = edad;
            } catch (NumberFormatException e) {
                System.out.println("Ingresa un número válido para la edad.");
            }
        } while (!controlador.validarEdad(edad));

        do {
            System.out.print("Email: ");
            email = br.readLine();
            infoCliente[4] = email;
        } while (!controlador.validarEmail(email));

        do {
            System.out.print("Usuario (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
            infoCliente[5] = usuario;
        } while (!controlador.validarUsuario(usuario));

        do {
            System.out.println("Contraseña: ");
            System.out.println("(debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            String contrasenaHasheada = hashearContrasena(contrasena);
            infoCliente[6] = contrasenaHasheada;
        } while (!controlador.validarContrasena(contrasena));


        return infoCliente;
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