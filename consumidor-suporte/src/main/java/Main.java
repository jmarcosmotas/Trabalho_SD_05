public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("==================================");
        System.out.println(" CONSUMIDOR DE SENTIMENTOS");
        System.out.println("==================================");

        // 1. Treina o modelo e salva em modelos/modelo_sentimento.ser
        System.out.println("[1/3] Iniciando treinamento...");
        TreinarModelo.executar();

        // 2. Carrega o modelo salvo e inicia o consumidor
        System.out.println("[2/3] Iniciando consumidor RabbitMQ...");
        ConsumerClassificarSuporte.iniciar();

    }
}