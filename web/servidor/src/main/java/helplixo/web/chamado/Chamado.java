package helplixo.web.chamado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um chamado, com os mesmos campos do sistema atual.
 * nSemin vazio quer dizer "sem máquina".
 * naoComputavel = fica fora das estatísticas.
 * andamento = as anotações, da mais antiga para a mais nova.
 * fechamento = o resultado final; só existe enquanto o chamado está Fechado.
 */
public record Chamado(
        String numero,
        String nSemin,
        String setor,
        String contato,
        String email,
        String problema,
        String descricao,
        String observacao,
        String tecnico,
        String area,
        StatusChamado status,
        String urgencia,
        String ocorrencia,
        boolean naoComputavel,
        LocalDateTime abertoEm,
        List<Anotacao> andamento,
        Fechamento fechamento) {

    public Chamado {
        andamento = andamento == null ? List.of() : List.copyOf(andamento);
    }

    public boolean semMaquina() {
        return nSemin == null || nSemin.isBlank();
    }

    /** Uma linha do andamento: o que foi feito, quando e por quem. */
    public record Anotacao(String texto, LocalDateTime em, String tecnico) {}

    /** O resultado de quem fechou. */
    public record Fechamento(String descricao, LocalDateTime em, String tecnico) {}

    /** Cópia deste chamado com outro status, andamento, fechamento e técnico. */
    Chamado com(StatusChamado novoStatus, List<Anotacao> novoAndamento, Fechamento novoFechamento, String novoTecnico) {
        return new Chamado(numero, nSemin, setor, contato, email, problema, descricao, observacao, novoTecnico, area,
                novoStatus, urgencia, ocorrencia, naoComputavel, abertoEm, novoAndamento, novoFechamento);
    }

    List<Anotacao> andamentoMais(List<Anotacao> novas) {
        List<Anotacao> lista = new ArrayList<>(andamento);
        lista.addAll(novas);
        return lista;
    }
}
