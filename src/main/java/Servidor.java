import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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
        System.out.println("Servidor arrancado..");

        while (true) {
            //se conecta cliente con servidor
            cliente = servidor.accept();
            System.out.println("Nuevo cliente conectado");
            HiloServidor hiloServidor = new HiloServidor(cliente);
            hiloServidor.start();
        }
    }

}
