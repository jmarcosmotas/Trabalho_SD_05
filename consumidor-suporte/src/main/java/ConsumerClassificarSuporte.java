import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConsumerClassificarSuporte {

    private static final String CAMINHO_MODELO ="modelos/modelo_suporte.ser";

    private static final String EXCHANGE ="messages.exchange";

    private static final String FILA_SUPORTE ="fila.suporte";

    private static final String ROUTING_KEY_SUPORTE ="support.request";

    private static final String EXCHANGE_DLQ ="messages.dlx";

    private static final String FILA_DLQ ="fila.suporte.dlq";

    private static final String ROUTING_KEY_DLQ ="suporte.dead";

    public static void iniciar() throws Exception {

        // 1. Verifica se o modelo de suporte existe.
        if (!Files.exists(Path.of(CAMINHO_MODELO))) {
            throw new IllegalStateException("Modelo de suporte não encontrado. " + "Execute o treinamento primeiro.");
        }

        // 2. Carrega o modelo salvo.
        TreinarModelo.ModeloSuporte modelo;

        try (ObjectInputStream entrada = new ObjectInputStream(
                new FileInputStream(CAMINHO_MODELO))) {

            modelo = (TreinarModelo.ModeloSuporte)
                    entrada.readObject();
        }

        System.out.println(
                "[SUPORTE] Modelo carregado com sucesso!"
        );

        // 3. Configura a conexão com o RabbitMQ.
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(System.getenv("RABBITMQ_HOST"));
        factory.setUsername(System.getenv("RABBITMQ_USER"));
        factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));

        // 4. Abre a conexão e cria o canal.
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 5. Declara o exchange principal.
        channel.exchangeDeclare(EXCHANGE, "topic", true);

        // 6. Declara o exchange de mensagens mortas.
        channel.exchangeDeclare(EXCHANGE_DLQ, "direct", true);

        // 7. Declara a fila que armazenará mensagens com falha.
        channel.queueDeclare(FILA_DLQ, true, false, false, null);

        // 8. Associa a DLQ ao exchange de mensagens mortas.
        channel.queueBind(FILA_DLQ, EXCHANGE_DLQ, ROUTING_KEY_DLQ);

        // 9. Configura a fila principal para utilizar a DLX.
        Map<String, Object> argumentos = new HashMap<>();

        argumentos.put("x-dead-letter-exchange", EXCHANGE_DLQ);

        argumentos.put("x-dead-letter-routing-key", ROUTING_KEY_DLQ);

        // 10. Declara a fila de suporte com a configuração de DLX.
        channel.queueDeclare(FILA_SUPORTE,true, false, false, argumentos);

        // 11. Associa a fila de suporte ao exchange principal.
        channel.queueBind(FILA_SUPORTE, EXCHANGE, ROUTING_KEY_SUPORTE);

        System.out.println("[SUPORTE] DLX configurada: " + EXCHANGE_DLQ);

        System.out.println("[SUPORTE] DLQ configurada: " + FILA_DLQ);

        System.out.println("[SUPORTE] Aguardando solicitações...");

        // 12. Recebe e classifica as solicitações.
        DeliverCallback callback = (tag, delivery) -> {

            long deliveryTag = delivery.getEnvelope().getDeliveryTag();

            String mensagem = new String(
                    delivery.getBody(),
                    StandardCharsets.UTF_8
            );

            try {
                String resultado = modelo.classificar(mensagem);

                System.out.println("\n[SOLICITAÇÃO] " + mensagem);

                System.out.println("[CATEGORIA] " + resultado);

                // Confirma a mensagem somente após o processamento.
                channel.basicAck(deliveryTag, false);

            } catch (Exception e) {

                System.err.println("[SUPORTE] Erro ao classificar solicitação: " + e.getMessage());

                // Rejeita a mensagem e solicita o encaminhamento à DLQ.
                channel.basicNack(deliveryTag, false, false);

                System.err.println("[SUPORTE] Mensagem encaminhada para a DLQ.");
            }
        };

        // 13. Consome mensagens com confirmação manual.
        channel.basicConsume( FILA_SUPORTE, false, callback, tag -> {});
    }
}