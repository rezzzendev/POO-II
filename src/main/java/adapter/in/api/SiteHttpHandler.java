package adapter.in.api;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

/** Somente arquivos públicos explicitamente permitidos. */
public class SiteHttpHandler implements HttpHandler {
    private final Map<String, String> arquivos =
            Map.of(
                    "/",
                    "index.html",
                    "/index.html",
                    "index.html",
                    "/api.js",
                    "api.js",
                    "/app.js",
                    "app.js",
                    "/style.css",
                    "style.css");

    public void handle(HttpExchange e) throws IOException {
        String nome = arquivos.get(e.getRequestURI().getPath());
        if (!e.getRequestMethod().equals("GET")
                || nome == null
                || !Files.isRegularFile(Path.of("web", nome))) {
            HttpJson.responder(e, 404, HttpJson.erro("Página não encontrada."));
            return;
        }
        byte[] bytes = Files.readAllBytes(Path.of("web", nome));
        String tipo =
                nome.endsWith(".js")
                        ? "text/javascript"
                        : nome.endsWith(".css") ? "text/css" : "text/html";
        e.getResponseHeaders().set("Content-Type", tipo + "; charset=utf-8");
        e.sendResponseHeaders(200, bytes.length);
        try (var out = e.getResponseBody()) {
            out.write(bytes);
        }
    }
}
