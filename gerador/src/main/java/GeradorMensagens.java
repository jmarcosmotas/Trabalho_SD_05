import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeradorMensagens {

    private static final String EXCHANGE = "messages.exchange";
    private static final String ROUTING_KEY_AVALIACAO = "review.product";
    private static final String ROUTING_KEY_SUPORTE = "support.request";

    private static final long INTERVALO_MS = 150;

    public static void main(String[] args) {

        try {
            // 1. Carrega as mensagens dos arquivos TXT, mantendo a ordem das linhas.
            List<String> avaliacoes =
                    LeitorMensagens.lerArquivo("mensagens/avaliacoes.txt");

            List<String> suportes =
                    LeitorMensagens.lerArquivo("mensagens/suporte.txt");

            System.out.println("[GERADOR] Arquivos carregados.");
            System.out.println("[GERADOR] Avaliações: " + avaliacoes.size());
            System.out.println("[GERADOR] Suporte: " + suportes.size());

            // 2. Configura a conexão com o RabbitMQ.
            ConnectionFactory factory = new ConnectionFactory();

            String host = System.getenv("RABBITMQ_HOST");
            String usuario = System.getenv("RABBITMQ_USER");
            String senha = System.getenv("RABBITMQ_PASSWORD");

            factory.setHost(host);
            factory.setUsername(usuario);
            factory.setPassword(senha);

            // 3. Estabelece a conexão e cria o canal.
            try (Connection conexao = factory.newConnection();
                 Channel canal = conexao.createChannel()) {

                // Declara o exchange do tipo topic.
                canal.exchangeDeclare(EXCHANGE, "topic", true);

                long contador = 0;

                System.out.println("[GERADOR] Conectado ao RabbitMQ.");
                System.out.println("[GERADOR] Iniciando envio das mensagens em ordem...");

                // 4. Percorre os dois arquivos sem sortear mensagens.
                int total = Math.max(avaliacoes.size(), suportes.size());

                for (int i = 0; i < total; i++) {

                    // 5. Envia a próxima avaliação, se houver.
                    if (i < avaliacoes.size()) {

                        String mensagem = avaliacoes.get(i);

                        enviarMensagem(
                                canal,
                                mensagem,
                                ROUTING_KEY_AVALIACAO,
                                "AVALIACAO",
                                ++contador
                        );

                        Thread.sleep(INTERVALO_MS);
                    }

                    // 6. Envia a próxima mensagem de suporte, se houver.
                    if (i < suportes.size()) {

                        String mensagem = suportes.get(i);

                        enviarMensagem(
                                canal,
                                mensagem,
                                ROUTING_KEY_SUPORTE,
                                "SUPORTE",
                                ++contador
                        );

                        Thread.sleep(INTERVALO_MS);
                    }
                }

                System.out.println("[GERADOR] Todas as mensagens foram enviadas.");
                System.out.println("[GERADOR] Total enviado: " + contador);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[GERADOR] Execução interrompida.");

        } catch (Exception e) {
            System.err.println("[GERADOR] Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Publica uma mensagem no RabbitMQ.
    private static void enviarMensagem(
            Channel canal,
            String mensagem,
            String routingKey,
            String tipo,
            long contador
    ) throws Exception {

        byte[] corpo = mensagem.getBytes(StandardCharsets.UTF_8);

        canal.basicPublish(EXCHANGE, routingKey, null, corpo);

        System.out.printf(
                "[GERADOR] #%d | Tipo: %s | Routing key: %s | Mensagem: %s%n",
                contador,
                tipo,
                routingKey,
                mensagem
        );
    }
}

