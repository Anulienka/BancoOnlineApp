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
                        System.out.println("\n****** FORMULARIO DE REGISTRACION DE NUEVO CLIENTE ******");
                        oos.writeObject(1);
                        //array con toda la informacion de cliente
                        clienteBancoInfo = registrarClienteBanco();
                        // Envia el array con informacion de cliente que se quiere registrar
                        oos.writeObject(clienteBancoInfo);


                        //****** INFO CIFRADA ***
                        //cada informacion se cifra y envia al servidor
//                        for (int i = 0; i < clienteBancoInfo.length; i++) {
//                            byte[] datoCifrado = cifrarInfoUsuario(clienteBancoInfo[i])
//                            oos.writeObject(datoCifrado)
//                        }

                        //FIRMA DIGITAL - documento firmado digitalmente por el servidor
                        //firmar documento
                        firmarDocumento(ois, oos);
                        //recibe mensaje del banco
                        mensajeBanco = ois.readUTF();
                        if (mensajeBanco.equals("R")) {
                            System.out.println("¡Usuario ha sido registrado con éxito y puede usar la aplicación del banco!\n");
                            //no pongo break aqui, porque usuario puede iniciar sesion si esta registrado, sin que elije en menu
                        } else if (mensajeBanco.equals("E")) {
                            System.out.println("Error en registrar.");
                            break;
                        } else if (mensajeBanco.equals("EX")) {
                            System.out.println("El usuario ya está en uso. Por favor, elige otro.\n");
                            break;
                        } else if (mensajeBanco.equals("NS")) {
                            System.out.println("No se han aceptado normas de banco, cliente no ha sido registrado.\n");
                            break;
                        }else if (mensajeBanco.equals("EXEMAIL")) {
                            System.out.println("Usuario con mismo email ya existe\n");
                            break;
                        }
                        else if (mensajeBanco.equals("EXDNI")) {
                            System.out.println("Usuario con mismo DNI ya existe\n");
                            break;
                        }

                    case 2:
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
                                    System.out.println("3. Hacer transferencia");

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
                                            numCuentaCifrada = cifrar(cuentaUsuario);
                                            oos.writeObject(numCuentaCifrada);
                                            System.out.println("Saldo actual de la cuenta: " + ois.readDouble() + "\n");
                                            break;
                                        case 3:
                                            oos.writeObject(5);
                                            numerosCuentasUsuario = (List<String>) ois.readObject();
                                            cuentaUsuario = elegirCuentaUsuario(numerosCuentasUsuario);
                                            numCuentaCifrada = cifrar(cuentaUsuario);
                                            //primero envio numero de cuenta cifrada
                                            oos.writeObject(numCuentaCifrada);
                                            //luego envio inporte cifrado
                                            double importe = insertarImporte();
                                            byte[] importeCifrado = cifrar(String.valueOf(importe));
                                            oos.writeObject(importeCifrado);
                                            // codigo cifrado desde servidor, que el usuario debe leerlo, descifrarlo e insertarlo correctamente.
                                            byte[] codigo = (byte[]) ois.readObject();
                                            //descifra el codigo
                                            String codigoServidor = descifrar(codigo);
                                            System.out.println(codigoServidor);
                                            boolean codigoValido = insertarComprobarCodigo(codigoServidor);
                                            //envia al servidor si es valido o no el codigo
                                            oos.writeBoolean(codigoValido);
                                            oos.flush();
                                            //recibe mensaje del servidor, si se ha hecho importe o no
                                            System.out.println(ois.readUTF());
                                            break;

                                    }

                                } catch (NumberFormatException | IOException | NoSuchPaddingException |
                                         IllegalBlockSizeException | BadPaddingException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        } else if (valido.equals("NV")) {
                            System.out.println("Contraseña incorrecta.");
                        } else {
                            System.out.println("No existe usuario con Username insertado.");
                        }
                        break;
                }


            } catch (NumberFormatException | IOException | ClassNotFoundException | NoSuchAlgorithmException |
                     SignatureException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }

        cerrarFlujos(cliente, oos, ois);
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
     * Inserta y comprueba un código ingresado por el usuario con codigo que ha enviaro servidor.
     * El usuario tiene 3 intentos para escribir el código correcto
     *
     * @param codigoServidor El código proporcionado por el servidor para comparación.
     * @return true si el código ingresado coincide con el código del servidor, false si codigos con diferentes.
     * @throws IOException Si ocurre un error de entrada/salida durante la lectura.
     */
    private boolean insertarComprobarCodigo(String codigoServidor) throws IOException {
        boolean codigoValido = false;
        String codigo = "";
        System.out.println("Inserta codigo de la pantalla:");

        int contador = 0;

        while (!codigoValido) {
            //usuario puede insertar codigo 3 veces
            if (contador <= 2) {
                codigo = br.readLine();
                //comprueba si tiene 4 numeros
                boolean formatoValidoCodigo = controlador.validarCodigo(codigo);
                if (formatoValidoCodigo) {
                    if (codigoServidor.equals(codigo)) {
                        codigoValido = true;
                    } else {
                        contador++;
                    }
                }
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
                //mensaje firmado
                byte[] firma = signature.sign();
                //envia mensaje firmado al servidor
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
        String numCuenta = null;

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
            } catch (IOException e) {
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
        ClienteBanco clienteBanco = new ClienteBanco();

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

        //al HashMap anado usuario y contrasena que quiero enviar al servidor
        credenciales[0] = usuario;
        credenciales[1] = contrasenaHasheada;

        return credenciales;
    }

    /**
     * Metodo para registrar a un nuevo cliente en el banco solicitando y validando su información personal.
     *
     * @return Un array que contiene la información del cliente, en el orden: Nombre, Apellido, Edad, Email, Usuario, Contraseña.
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