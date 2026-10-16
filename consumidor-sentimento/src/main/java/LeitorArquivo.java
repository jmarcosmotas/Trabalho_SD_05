import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LeitorArquivo {

    public static List<String[]> lerCSV(String caminho)
            throws IOException {

        List<String[]> linhas = new ArrayList<>();

        try (BufferedReader leitor = Files.newBufferedReader(
                Path.of(caminho), StandardCharsets.UTF_8)) {

            String linha;
            boolean cabecalho = true;

            while ((linha = leitor.readLine()) != null) {

                // Ignora a primeira linha, que contém os nomes das colunas
                if (cabecalho) {
                    cabecalho = false;
                    continue;
                }

                if (linha.isBlank()) {
                    continue;
                }

                List<String> colunas = new ArrayList<>();
                StringBuilder campo = new StringBuilder();
                boolean entreAspas = false;

                for (int i = 0; i < linha.length(); i++) {
                    char caractere = linha.charAt(i);

                    if (caractere == '"') {
                        if (entreAspas
                                && i + 1 < linha.length()
                                && linha.charAt(i + 1) == '"') {
                            campo.append('"');
                            i++;
                        } else {
                            entreAspas = !entreAspas;
                        }
                    } else if (caractere == ',' && !entreAspas) {
                        colunas.add(campo.toString().trim());
                        campo.setLength(0);
                    } else {
                        campo.append(caractere);
                    }
                }

                colunas.add(campo.toString().trim());

                if (colunas.size() >= 2
                        && !colunas.get(0).isBlank()
                        && !colunas.get(1).isBlank()) {

                    linhas.add(new String[] {
                        colunas.get(0),
                        colunas.get(1).toLowerCase()
                    });
                }
            }
        }

        if (linhas.isEmpty()) {
            throw new IOException(
                "Nenhum exemplo válido encontrado no CSV: " + caminho
            );
        }

        return linhas;
    }
}