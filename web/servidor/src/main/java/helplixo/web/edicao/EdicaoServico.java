package helplixo.web.edicao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Modo edição conferido no servidor.
 * - A senha vem de helplixo.edicao.senha, que lê a variável de ambiente HELPLIXO_SENHA_EDICAO.
 *   Sem a variável, o modo edição fica desligado para todos.
 * - Senha certa gera uma chave de edição aleatória, válida por algumas horas.
 * - 5 erros seguidos do mesmo computador: espera 1 minuto para tentar de novo.
 */
@Service
public class EdicaoServico {

    static final int ERROS_ATE_ESPERAR = 5;
    static final Duration ESPERA_APOS_ERROS = Duration.ofMinutes(1);

    private final byte[] senha;
    private final Duration validade;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Instant> chaves = new ConcurrentHashMap<>();
    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();

    private record Tentativas(int erros, Instant esperarAte) {}

    /** Resposta de quem tentou entrar. */
    public record Entrada(String chave, Instant validaAte) {}

    /** Algo impediu a entrada; o motivo vai para a tela. */
    public static class EntradaRecusada extends RuntimeException {
        private final Motivo motivo;
        public enum Motivo { DESLIGADO, SENHA_ERRADA, ESPERAR }
        EntradaRecusada(Motivo motivo, String mensagem) {
            super(mensagem);
            this.motivo = motivo;
        }
        public Motivo motivo() {
            return motivo;
        }
    }

    public EdicaoServico(@Value("${helplixo.edicao.senha:}") String senha,
                         @Value("${helplixo.edicao.validade-horas:8}") long horas,
                         Clock relogio) {
        this.senha = senha == null ? new byte[0] : senha.getBytes(StandardCharsets.UTF_8);
        this.validade = Duration.ofHours(horas);
        this.relogio = relogio;
    }

    public boolean configurado() {
        return senha.length > 0;
    }

    /** Confere a senha. origem = quem tentou (o endereço do computador). */
    public Entrada entrar(String tentativa, String origem) {
        if (!configurado()) {
            throw new EntradaRecusada(EntradaRecusada.Motivo.DESLIGADO,
                    "O modo edição não está configurado neste servidor. Peça para definir a variável HELPLIXO_SENHA_EDICAO.");
        }
        Instant agora = relogio.instant();
        Tentativas t = tentativas.getOrDefault(origem, new Tentativas(0, Instant.MIN));
        if (agora.isBefore(t.esperarAte())) {
            long seg = Math.max(1, Duration.between(agora, t.esperarAte()).toSeconds());
            throw new EntradaRecusada(EntradaRecusada.Motivo.ESPERAR, "Muitas tentativas erradas. Tente de novo em " + seg + " segundos.");
        }
        byte[] digitada = tentativa == null ? new byte[0] : tentativa.getBytes(StandardCharsets.UTF_8);
        // comparação em tempo constante: o tempo de resposta não revela quantas letras estão certas
        if (!MessageDigest.isEqual(senha, digitada)) {
            int erros = t.erros() + 1;
            if (erros >= ERROS_ATE_ESPERAR) {
                tentativas.put(origem, new Tentativas(0, agora.plus(ESPERA_APOS_ERROS)));
                throw new EntradaRecusada(EntradaRecusada.Motivo.ESPERAR, "Senha incorreta. Foram " + ERROS_ATE_ESPERAR + " erros seguidos: espere 1 minuto para tentar de novo.");
            }
            tentativas.put(origem, new Tentativas(erros, Instant.MIN));
            throw new EntradaRecusada(EntradaRecusada.Motivo.SENHA_ERRADA, "Senha incorreta. Confira e tente de novo.");
        }
        tentativas.remove(origem);
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String chave = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ate = agora.plus(validade);
        chaves.put(chave, ate);
        return new Entrada(chave, ate);
    }

    public void sair(String chave) {
        if (chave != null) chaves.remove(chave);
    }

    /** Até quando a chave vale; vazio se não existe ou já venceu. */
    public Optional<Instant> validaAte(String chave) {
        if (chave == null || chave.isBlank()) return Optional.empty();
        Instant ate = chaves.get(chave);
        if (ate == null) return Optional.empty();
        if (!relogio.instant().isBefore(ate)) {
            chaves.remove(chave);
            return Optional.empty();
        }
        return Optional.of(ate);
    }

    /** Para as ações de edição: recusa se a chave não vale. */
    public void exigir(String chave) {
        if (validaAte(chave).isEmpty()) {
            throw new SemEdicaoException();
        }
    }

    /** Ação de edição sem o modo edição ligado. */
    public static class SemEdicaoException extends RuntimeException {
        public SemEdicaoException() {
            super("Entre no modo edição (senha de edição) para fazer isto.");
        }
    }
}
