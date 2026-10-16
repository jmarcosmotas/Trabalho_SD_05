import smile.classification.DiscreteNaiveBayes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TreinarModelo {

    private static final String CSV ="dados/treinamento_sentimento.csv";

    private static final String CAMINHO_MODELO ="modelos/modelo_sentimento.ser";

    public static void executar() throws Exception {

        System.out.println("Lendo dados de treinamento...");

        List<String[]> dados = LeitorArquivo.lerCSV(CSV);

        ModeloSentimento modelo = treinar(dados);

        Files.createDirectories(Path.of("modelos"));

        try (ObjectOutputStream saida = new ObjectOutputStream(
                new FileOutputStream(CAMINHO_MODELO))) {

            saida.writeObject(modelo);
        }

        System.out.println("Treinamento concluído!");
        System.out.println("Total de exemplos: " + dados.size());
        System.out.println("Total de palavras: " + modelo.vocabulario.size());
        System.out.println("Classes: " + modelo.classes);
        System.out.println("Modelo salvo em: " + CAMINHO_MODELO);
    }

    private static ModeloSentimento treinar(List<String[]> dados) {

        ModeloSentimento resultado = new ModeloSentimento();

        // 1. Cria o vocabulário e identifica as classes
        for (String[] linha : dados) {
            String texto = linha[0];
            String classe = linha[1].trim().toLowerCase();

            if (!resultado.classes.contains(classe)) {
                resultado.classes.add(classe);
            }

            for (String palavra : tokenizar(texto)) {
                if (!resultado.vocabulario.containsKey(palavra)) {
                    resultado.vocabulario.put(
                        palavra,
                        resultado.vocabulario.size()
                    );
                }
            }
        }

        // Verifica se existem categorias e palavras para treinar.
        if (resultado.classes.size() < 2
                || resultado.vocabulario.isEmpty()) {
            throw new IllegalArgumentException(
                "O CSV precisa ter pelo menos duas classes "
                + "e um vocabulário não vazio."
            );
        }

        // 2. Cria o modelo Naive Bayes
        resultado.modelo = new DiscreteNaiveBayes(
            DiscreteNaiveBayes.Model.MULTINOMIAL,
            resultado.classes.size(),
            resultado.vocabulario.size()
        );

        // 3. Treina usando cada exemplo do CSV
        for (String[] linha : dados) {
            String texto = linha[0];
            String classe = linha[1].trim().toLowerCase();

            int[] vetor = resultado.gerarVetor(texto);
            int rotulo = resultado.classes.indexOf(classe);

            resultado.modelo.update(vetor, rotulo);
        }

        return resultado;
    }

    // Separa o texto em palavras e remove pontuação.
    private static String[] tokenizar(String texto) {
        String normalizado = texto.toLowerCase()
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim();

        if (normalizado.isEmpty()) {
            return new String[0];
        }

        return normalizado.split("\\s+");
    }

    // Guarda o modelo, o vocabulário e as categorias.
    public static class ModeloSentimento implements Serializable {

        private static final long serialVersionUID = 1L;

        private DiscreteNaiveBayes modelo;
        private Map<String, Integer> vocabulario =
            new LinkedHashMap<>();
        private List<String> classes = new ArrayList<>();

        private int[] gerarVetor(String texto) {

            int[] vetor = new int[vocabulario.size()];

            for (String palavra : tokenizar(texto)) {
                Integer indice = vocabulario.get(palavra);

                if (indice != null) {
                    vetor[indice]++;
                }
            }

            return vetor;
        }

        public String classificar(String texto) {

            int[] vetor = gerarVetor(texto);

            boolean temPalavraConhecida = false;

            for (int frequencia : vetor) {
                if (frequencia > 0) {
                    temPalavraConhecida = true;
                    break;
                }
            }

            if (!temPalavraConhecida) {
                return "INDETERMINADO";
            }

            int previsao = modelo.predict(vetor);

            return classes.get(previsao).toUpperCase();
        }
    }
}