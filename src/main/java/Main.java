import adapter.in.api.EventoHttpHandler;
import adapter.out.persistence.evento.EventoRepositoryJdbc;
import application.evento.EventoRepository;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        EventoRepository repository = new EventoRepositoryJdbc();

        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);
        servidor.createContext("/eventos", new EventoHttpHandler(repository));
        servidor.setExecutor(null);
        servidor.start();

        System.out.println("API no ar em http://localhost:8080/eventos");
    }
}
