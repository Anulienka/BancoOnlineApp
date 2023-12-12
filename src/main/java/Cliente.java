import controlador.Controlador;
import model.ClienteBanco;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Arrays;
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

    public static void main(String[] args) throws IOException {
        // Crea el cliente
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
            KeyPairGenerator clavesUsuario = KeyPairGenerator.getInstance("RSA");
            KeyPair par = clavesUsuario.generateKeyPair();
            privada = par.getPrivate();
            publica = par.getPublic();
            //mandamos la clave del cliente al servidor
            oos.writeObject(publica);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
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
//                            cifrarInfoUsuario(clienteBancoInfo[i], oos);
//                        }


                        //FIRMA DIGITAL - documento firmado digitalmente por el servidor
                        //firmar documento
                        firmarDocumento(ois, oos);
                        //recibe mensaje del banco
                        mensajeBanco = ois.readUTF();
                        if (mensajeBanco.equals("R")) {
                            System.out.println("Te has registrado correctamente, puedes usar app de banco.\n");
                            //no pongo break aqui, porque usuario puede iniciar sesion si esta registrado, sin que elije en menu
                        } else if (mensajeBanco.equals("E")) {
                            System.out.println("Error en registrar");
                            break;
                        } else if (mensajeBanco.equals("EX")) {
                            System.out.println("Ya existe usuario con mismo Username.\n");
                            break;
                        } else if (mensajeBanco.equals("NS")) {
                            System.out.println("No se ha firmado documento, cliente no ha sido registrado.\n");
                            break;
                        }

                    case 2:
                        System.out.println("\n****** INICIAR SESION ******");
                        System.out.println("Para poder realizar operaciones bancarias, \nnecesitas iniciar sesion insertando su usuario y contraseña.\n");
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
                            while (true){

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
                                            List<String> numeroCuentasUsuario = (List<String>) ois.readObject();
                                            String cuentaUsuario = elegirCuentaUsuario(numeroCuentasUsuario);
                                            byte[] numCuentaCifrada = cifrarCuentaUsuario(cuentaUsuario);
                                            oos.writeObject(numCuentaCifrada);
                                            System.out.println("Saldo actual de la cuenta: " + ois.readDouble() + "\n");
                                            break;
                                        case 3:
                                            oos.writeObject(5);

                                    }

                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                } catch (NoSuchPaddingException e) {
                                    throw new RuntimeException(e);
                                } catch (IllegalBlockSizeException e) {
                                    throw new RuntimeException(e);
                                } catch (BadPaddingException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        } else if(valido.equals("NV")) {
                            System.out.println("Contraseña incorrecta.");
                        }else{
                            System.out.println("No existe usuario con Username insertado.");
                        }
                        break;
                }


            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void firmarDocumento(ObjectInputStream ois, ObjectOutputStream oos) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        //recibe documento desde servidor
        String normasBanco = ois.readObject().toString();
        System.out.println(normasBanco);
        char opcion = 'O';

        while (true){
            System.out.println("\nAcceptas normas de banco? (S/N)");
            opcion = br.readLine().toUpperCase().charAt(0);

            if(opcion == 'S'){
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

    private void cifrarInfoUsuario(String infoUsuario, ObjectOutputStream oos) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicaServidor);
        //directamente cifrarlo en un array de bytes, y no hacer conversiones a string
        byte[] infoUsuarioCifrado = cipher.doFinal(infoUsuario.getBytes());
        oos.writeObject(infoUsuarioCifrado);
    }

    private byte[] cifrarCuentaUsuario(String numCuentaUsuario) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicaServidor);
        //directamente cifrarlo en un array de bytes, y no hacer conversiones a string
        byte[] numCuentaCifrada = cipher.doFinal(numCuentaUsuario.getBytes());
        return numCuentaCifrada;
    }

    private String elegirCuentaUsuario(List<String> cuentasUsuario) {
        int contador = 1;
        boolean opcionValida = true;
        int opcionCuenta = 0;
        String numCuenta = null;

        for (String c: cuentasUsuario) {
            System.out.println(contador + ". " + c);
            contador++;
        }

        while (opcionValida) {
            try {
                System.out.println("Elije cuenta bancaria:");
                opcionCuenta = Integer.parseInt(br.readLine());
                if (opcionCuenta < 1 || opcionCuenta > cuentasUsuario.size()) {
                    System.out.println("Opcion inválida.");
                } else {
                    //retorna empleado elegido
                    numCuenta = cuentasUsuario.get(opcionCuenta - 1);
                    opcionValida = false;
                }
            } catch (Exception e) {
                System.out.println("Opcion inválida. Intenta otra vez insertando numero.");
            }
        }

        return numCuenta;
    }

    private String[] iniciarSesion() throws IOException {
        String contrasena;
        String usuario;
        String[] credenciales = new String[2];
        ClienteBanco clienteBanco = new ClienteBanco();

        do {
            System.out.println("USUARIO (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
        } while (!controlador.validarUsuario(usuario));

        do {
            System.out.print("CONTRASEÑA: ");
            System.out.println("(Debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
        } while (!controlador.validarContrasena(contrasena));

        //al HashMap anado usuario y contrasena que quiero enviar al servidor
        credenciales[0] = usuario;
        credenciales[1] = contrasena;

        return credenciales;
    }


    private String[] registrarClienteBanco() throws IOException {
        String[] infoCliente = new String[6];

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
            System.out.print("Edad: ");
            try {
                edad = br.readLine();
                infoCliente[2] = edad;
            } catch (NumberFormatException e) {
                System.out.println("Ingresa un número válido para la edad.");
            }
        } while (!controlador.validarEdad(edad));

        do {
            System.out.print("Email: ");
            email = br.readLine();
            infoCliente[3] = email;
        } while (!controlador.validarEmail(email));

        do {
            System.out.print("Usuario (debe que tener 6 caracteres alfanumericos): ");
            usuario = br.readLine();
            infoCliente[4] = usuario;
        } while (!controlador.validarUsuario(usuario));

        do {
            System.out.print("Contraseña: ");
            System.out.println("(Debe que tener 10 caracteres: por lo menos 1 digito, 1 mayuscula y 1 minuscula)");
            contrasena = br.readLine();
            infoCliente[5] = contrasena;
        } while (!controlador.validarContrasena(contrasena));


        return infoCliente;
    }

}