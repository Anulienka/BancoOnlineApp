import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Credenciales {

    private static final String ENCODING_TYPE = "UTF-8";

    public void registrarCredenciales(String usuario, String contrasena) throws IOException, NoSuchAlgorithmException {

        FileOutputStream file = new FileOutputStream(" credenciales.cre");
        ObjectOutputStream oos = new ObjectOutputStream(file);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        byte[] contrasenaByte = contrasena.getBytes();
        md.update(contrasenaByte);
        //esto es contrasena HASH
        byte[] resumen = md.digest();
        //necesito usar Map para guardar usuario y contrasena en pichero
        Map<String, byte[]> credenciales = null;
        credenciales.put(usuario, resumen);
        //almaceno contrasena almacenada en HASH
        oos.writeObject(credenciales);
    }

    public boolean validarCredenciales(String usuario, String contrasena) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        byte[] contrasenaByteOtraVez = contrasena.getBytes();
        md.update(contrasenaByteOtraVez);
        //contrasena HASH para que se pueda comparar con contrasena guardada en HASH en archivo credenciales
        byte[] resumenContrasena = md.digest();

        //leo descde archivo todas las credenciales guardadas
        Map<String, byte[]> credenciales = cargarCredenciales();

        // Verificar si el usuario existe
        if (!credenciales.containsKey(usuario)) {
            //Usuario no encontrado
            return false;
        }else{
            byte[] resumenContrasenaGuardada = credenciales.get(usuario);
            //comparo resumen de archivo y contrasena que usuario inserta
            if (MessageDigest.isEqual(resumenContrasena, resumenContrasenaGuardada)) {
                //contrasenas son iguales
                return true;
            } else {
                //contrasenas no son iguales
                return false;
            }
        }
    }

    private Map<String, byte[]> cargarCredenciales() {

        try (FileInputStream fichero = new FileInputStream("credenciales.cre");
             ObjectInputStream ois = new ObjectInputStream(fichero)) {

            // Cargo las credenciales del archivo
            Object obj = ois.readObject();

            if (obj instanceof Map) {
                //devuelve las credenciales existentes
                return (Map<String, byte[]>) obj;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //si no hay credenciales guardadas, retorna null
        return new HashMap<>();
    }

    public String hashearContrasena(String contrasena) throws IOException, NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.reset();
        byte[] contrasenaByte = contrasena.getBytes();
        md.update(contrasenaByte);
        //esto es contrasena HASH
        byte[] resumen = md.digest();
        String contrasenaHash = resumen.toString();
        return contrasenaHash;
    }
}
