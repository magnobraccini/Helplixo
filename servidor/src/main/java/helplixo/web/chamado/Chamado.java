package helplixo.web.chamado;

import java.time.LocalDateTime;

/**
 * Um chamado, com os mesmos campos do sistema atual.
 * nSemin vazio quer dizer "sem máquina".
 * naoComputavel = fica fora das estatísticas.
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
        LocalDateTime abertoEm) {

    public boolean semMaquina() {
        return nSemin == null || nSemin.isBlank();
    }
}
