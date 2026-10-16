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

public class ConsumerClassificarSentimentos {

    private static final String CAMINHO_MODELO ="modelos/modelo_sentimento.ser";

    private static final String EXCHANGE = "messages.exchange";

    private static final String FILA_SENTIMENTO ="fila.sentimento";

    private static final String ROUTING_KEY_SENTIMENTO ="review.product";

    private static final String EXCHANGE_DLQ = "messages.dlx";

    private static final String FILA_DLQ = "fila.sentimento.dlq";

    private static final String ROUTING_KEY_DLQ = "sentimento.dead";

    public static void iniciar() throws Exception {

        // 1. Verifica se o modelo treinado existe.
        if (!Files.exists(Path.of(CAMINHO_MODELO))) {
            throw new IllegalStateException("Modelo não encontrado. " + "Execute primeiro o comando treinar.");
        }

        // 2. Carrega o modelo salvo, sem treinar novamente.
        TreinarModelo.ModeloSentimento modelo;

        try (ObjectInputStream entrada = new ObjectInputStream(
                new FileInputStream(CAMINHO_MODELO))) {

            modelo = (TreinarModelo.ModeloSentimento)
                    entrada.readObject();
        }

        System.out.println("[SENTIMENTO] Modelo carregado com sucesso!");

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

        // 7. Declara a fila de mensagens mortas.
        channel.queueDeclare(FILA_DLQ, true, false, false, null);

        // 8. Associa a DLQ ao exchange de mensagens mortas.
        channel.queueBind(FILA_DLQ, EXCHANGE_DLQ,ROUTING_KEY_DLQ);

        // 9. Configura a fila principal para usar a DLX.
        Map<String, Object> argumentos = new HashMap<>();

        argumentos.put("x-dead-letter-exchange", EXCHANGE_DLQ);

        argumentos.put("x-dead-letter-routing-key",ROUTING_KEY_DLQ);

        // 10. Declara a fila de sentimentos com a configuração de DLX.
        channel.queueDeclare(
                FILA_SENTIMENTO,
                true,
                false,
                false,
                argumentos
        );

        // 11. Associa a fila ao exchange principal.
        channel.queueBind(
                FILA_SENTIMENTO,
                EXCHANGE,
                ROUTING_KEY_SENTIMENTO
        );

        System.out.println(
                "[SENTIMENTO] DLX configurada: " + EXCHANGE_DLQ
        );

        System.out.println(
                "[SENTIMENTO] DLQ configurada: " + FILA_DLQ
        );

        System.out.println(
                "[SENTIMENTO] Aguardando avaliações de produtos..."
        );

        // 12. Recebe e classifica as mensagens.
        DeliverCallback callback = (tag, delivery) -> {

            long deliveryTag =
                    delivery.getEnvelope().getDeliveryTag();

            String mensagem = new String(delivery.getBody(), StandardCharsets.UTF_8);

            try {
                String resultado = modelo.classificar(mensagem);

                System.out.println("\n[AVALIAÇÃO] " + mensagem);

                System.out.println("[SENTIMENTO] " + resultado);

                // Confirma a mensagem somente após o sucesso.
                channel.basicAck(deliveryTag, false);

            } catch (Exception e) {

                System.err.println("[SENTIMENTO] Erro ao classificar mensagem: " + e.getMessage());

                // Rejeita a mensagem sem reencaminhá-la
                // para a fila original.
                channel.basicNack(deliveryTag, false, false);

                System.err.println("[SENTIMENTO] Mensagem encaminhada para a DLQ.");
            }
        };

        // 13. Consome mensagens com confirmação manual.
        channel.basicConsume(FILA_SENTIMENTO, false, callback, tag -> {});
    }
}