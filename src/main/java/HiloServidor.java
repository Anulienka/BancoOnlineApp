import controlador.Controlador;
import model.ClienteBanco;
import model.Cuenta;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public HiloServidor(Socket cliente) throws IOException {
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

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
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
                        // usuarioRegistrarInfo = recogerInfoUsuario();
                        //crea objeto ClienteBanco, para que se puede luego insertar a BBDD
                        //ClienteBanco usuarioBanco = crearUsuario(usuarioRegistrarInfo);

                        usuarioRegistrarInfo = (String[]) ois.readObject();
                        //crea objeto ClienteBanco, para que se puede luego insertar a BBDD
                        ClienteBanco usuarioBanco = crearUsuario(usuarioRegistrarInfo);

                        System.out.println("Creando documento para firma digital");
                        //si usuario firma es valida, usuario se registra
                        boolean verificada = firmarDocumento();
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
                        List<Cuenta> cuentasCliente = controlador.buscarCuentasUsuario(usuarioActual);
                        List<String> numerosDeCuentas = listarNumerosDeCuenta(cuentasCliente);
                        oos.writeObject(numerosDeCuentas);
                        //servidor recibe numero de cuenta de usuario cifrada
                        byte[] numCuentaCifrada = (byte[]) ois.readObject();
                        //luego la descifra
                        String numCuentaUsuario = descifrarNumCuenta(numCuentaCifrada);
                        Double saldoDeCuenta = controlador.verSaldoDeCuenta(numCuentaUsuario);
                        //envia al cliente el saldo actual de la cuenta
                        oos.writeDouble(saldoDeCuenta);
                        oos.flush();
                        break;
                    case 5:
                        System.out.println("Haciendo transferencia");

                }


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
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            }
        }
        cierraFlujos(cliente, oos, ois);
    }

    private String[] recogerInfoUsuario() throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String[] usuarioInfo = new String[6];
        byte[] nombre = (byte[]) ois.readObject();
        String nombreUsuario = descifrafInfoUsuario(nombre);
        byte[] apellido = (byte[]) ois.readObject();
        String apellidoUsuario = descifrafInfoUsuario(apellido);
        byte[] edad = (byte[]) ois.readObject();
        String edadUsuario = descifrafInfoUsuario(edad);
        byte[] email = (byte[]) ois.readObject();
        String emailUsuario = descifrafInfoUsuario(email);
        byte[] usuario = (byte[]) ois.readObject();
        String username = descifrafInfoUsuario(usuario);
        byte[] contrasena = (byte[]) ois.readObject();
        String contrasenaUsuario = descifrafInfoUsuario(contrasena);
        usuarioInfo = new String[]{nombreUsuario, apellidoUsuario, edadUsuario, emailUsuario, username, contrasenaUsuario};
        return usuarioInfo;
    }

    private String descifrafInfoUsuario(byte[] info) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher descipher = Cipher.getInstance("RSA");
        descipher.init(Cipher.DECRYPT_MODE, privada);
        String infoUsuario = new String(descipher.doFinal(info));
        return infoUsuario;
    }

    private List<String> listarNumerosDeCuenta(List<Cuenta> cuentasCliente) {
        List<String> cuentas = new ArrayList<>();
        for (Cuenta c : cuentasCliente) {
            cuentas.add(c.getNumCuenta());
        }
        return cuentas;
    }

    private String descifrarNumCuenta(byte[] numCuentaCifrada) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher descipher = Cipher.getInstance("RSA");
        descipher.init(Cipher.DECRYPT_MODE, privada);
        String numCuentaDescifrada = new String(descipher.doFinal(numCuentaCifrada));
        return numCuentaDescifrada;
    }

    private void generarClaves() throws NoSuchAlgorithmException {
        //Primero se intercambian las claves publicas entre usuario y servidor
        //crea clave publica y privada para cada cliente
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        KeyPair par = keygen.generateKeyPair();
        privada = par.getPrivate();
        publica = par.getPublic();
    }

    private boolean firmarDocumento() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, ClassNotFoundException {
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

        } while (numerosTodasCuentas.contains(numeroCuenta));

        return numeroCuenta.toString();
    }

    private void cierraFlujos(Socket cliente, ObjectOutputStream oos, ObjectInputStream ois) {
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
