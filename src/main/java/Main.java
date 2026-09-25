import adapter.in.api.ServidorApi;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--demo")) {
            DadosDemonstracao.main(new String[0]);
            return;
        }
        int porta = Integer.getInteger("api.port", 8080);
        var servidor = ServidorApi.criar(porta);
        servidor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(0)));
        System.out.println("Site e API em http://localhost:" + porta);
    }
}
