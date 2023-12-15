import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Anna
 *
 * La clase Servidor escucha y acepta conexiones de múltiples clientes.
 */
public class Servidor {

    final int PUERTO = 4444;
    private ServerSocket servidor;
    private Socket cliente;

    public static void main(String[] args) throws IOException {

        Servidor s = new Servidor();
        s.initServer();
    }


    private void initServer() throws IOException {

        servidor = new ServerSocket(PUERTO);
        System.out.println("Servidor funcionando..");

        while (true) {
            //se conecta cliente con servidor
            cliente = servidor.accept();
            System.out.println("Nuevo cliente se ha conectado");
            HiloServidor hiloServidor = new HiloServidor(cliente);
            hiloServidor.start();
        }
    }

}

