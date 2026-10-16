import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LeitorMensagens {

    // Lê as mensagens de um arquivo de texto.
    public static List<String> lerArquivo(String caminho)
            throws IOException {

        Path arquivo = Path.of(caminho);

        // Verifica se o arquivo existe.
        if (!Files.exists(arquivo)) {
            throw new IOException("Arquivo não encontrado: " + arquivo.toAbsolutePath());
        }

        // Lê todas as linhas usando UTF-8.
        List<String> mensagens = Files.readAllLines(arquivo, StandardCharsets.UTF_8);

        // Remove espaços extras e linhas vazias.
        mensagens = mensagens.stream()
            .map(String::trim)
            .filter(mensagem -> !mensagem.isEmpty())
            .toList();

        // Impede a execução sem mensagens.
        if (mensagens.isEmpty()) {
            throw new IllegalStateException("O arquivo não possui mensagens: " + caminho);
        }

        return mensagens;
    }
}