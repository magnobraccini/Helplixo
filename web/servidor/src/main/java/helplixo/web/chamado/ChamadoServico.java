package helplixo.web.chamado;

import helplixo.web.RegraVioladaException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Regras do formulário único do chamado (aba Resultado).
 * O status escolhido decide o que acontece:
 * - Aberto / Em andamento: registra a anotação e muda o status; num chamado fechado, reabre.
 * - Fechado: fecha com o resultado final.
 */
@Service
public class ChamadoServico {

    static final int MINIMO_FECHAMENTO = 10;
    static final int TOLERANCIA_FUTURO_MIN = 5;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ChamadoRepositorio repositorio;
    private final Clock relogio;

    public ChamadoServico(ChamadoRepositorio repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    /** O que a tela manda ao salvar o formulário. */
    public record PedidoResultado(String status, String anotacao, LocalDateTime quando, String tecnico) {}

    /** O chamado como ficou, mais avisos que não impedem salvar (ex.: fora do expediente). */
    public record Resultado(Chamado chamado, List<String> avisos) {}

    public Resultado registrar(Chamado atual, PedidoResultado pedido) {
        List<String> erros = new ArrayList<>();
        StatusChamado novo = null;
        try {
            novo = pedido.status() == null ? null : StatusChamado.de(pedido.status());
        } catch (IllegalArgumentException e) {
            erros.add(e.getMessage());
        }
        if (novo == null && erros.isEmpty()) {
            erros.add("Escolha o status: Aberto, Em andamento ou Fechado.");
        }
        String texto = pedido.anotacao() == null ? "" : pedido.anotacao().trim();
        boolean fechar = novo == StatusChamado.FECHADO;

        // a anotação só é obrigatória para fechar
        if (fechar && texto.length() < MINIMO_FECHAMENTO) {
            erros.add("Para fechar, descreva o que foi feito (pelo menos " + MINIMO_FECHAMENTO + " letras).");
        }
        if (novo != null && !fechar && texto.isEmpty() && novo == atual.status()) {
            erros.add("Escreva uma anotação ou escolha outro status.");
        }
        if (pedido.tecnico() == null || pedido.tecnico().isBlank()) {
            erros.add(fechar ? "Escolha o técnico que realizou." : "Escolha o técnico.");
        }
        // horário: não pode estar no futuro nem antes da abertura
        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDateTime quando = pedido.quando();
        if (quando == null) {
            erros.add("Informe a data e a hora.");
        } else {
            if (quando.isAfter(agora.plusMinutes(TOLERANCIA_FUTURO_MIN))) {
                erros.add("A data e a hora não podem estar no futuro.");
            }
            if (quando.isBefore(atual.abertoEm())) {
                erros.add("A data e a hora precisam ser depois da abertura do chamado (" + DATA_HORA.format(atual.abertoEm()) + ").");
            }
        }
        if (!erros.isEmpty()) {
            throw new RegraVioladaException(erros);
        }

        String tecnico = pedido.tecnico().trim();
        Chamado salvo;
        if (fechar && atual.status() != StatusChamado.FECHADO) {
            salvo = atual.com(StatusChamado.FECHADO, atual.andamento(), new Chamado.Fechamento(texto, quando, tecnico), tecnicoOu(atual, tecnico));
        } else {
            boolean reabre = atual.status() == StatusChamado.FECHADO && !fechar;
            List<Chamado.Anotacao> novas = new ArrayList<>();
            if (reabre && atual.fechamento() != null) {
                Chamado.Fechamento f = atual.fechamento();
                novas.add(new Chamado.Anotacao("Fechado antes por " + f.tecnico() + ": " + f.descricao(), f.em(), f.tecnico()));
            }
            String linha = reabre ? "Reaberto" + (texto.isEmpty() ? "" : ": " + texto)
                    : (texto.isEmpty() ? "Status: " + novo.rotulo().toLowerCase() + "." : texto);
            novas.add(new Chamado.Anotacao(linha, quando, tecnico));
            // anotação num chamado que continua fechado mantém o fechamento
            Chamado.Fechamento fechamento = fechar ? atual.fechamento() : null;
            salvo = atual.com(novo, atual.andamentoMais(novas), fechamento, tecnicoOu(atual, tecnico));
        }
        repositorio.salvar(salvo);
        return new Resultado(salvo, avisosDoHorario(quando));
    }

    private static String tecnicoOu(Chamado c, String tecnico) {
        return c.tecnico() == null || c.tecnico().isBlank() ? tecnico : c.tecnico();
    }

    /** Expediente: segunda a sexta, 8h às 14h. Fora dele é só um aviso, nunca um bloqueio. */
    static List<String> avisosDoHorario(LocalDateTime quando) {
        DayOfWeek dia = quando.getDayOfWeek();
        int hora = quando.getHour();
        boolean noExpediente = dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY && hora >= 8 && hora < 14;
        return noExpediente ? List.of() : List.of("Fora do expediente (segunda a sexta, 8h às 14h). Confira se está certo.");
    }
}
