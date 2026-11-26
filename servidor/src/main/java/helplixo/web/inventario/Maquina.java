package helplixo.web.inventario;

import java.time.LocalDate;
import java.util.List;

/**
 * Uma máquina do inventário. O Nº Semin e o patrimônio são a identidade dela e nunca mudam.
 * Campos sem informação ficam null (a tela mostra "—").
 */
public record Maquina(
        String nSemin,
        String patrimonio,
        String tipo,
        String modelo,
        String serie,
        String processador,
        String memoria,
        String armazenamento,
        String sistema,
        String ip,
        String mac,
        String local,
        String usuario,
        String situacao,
        String observacao,
        Compra compra,
        List<Software> softwares,
        List<SistemaPmsl> sistemasPmsl,
        List<Conserto> consertos) {

    public Maquina {
        softwares = softwares == null ? List.of() : List.copyOf(softwares);
        sistemasPmsl = sistemasPmsl == null ? List.of() : List.copyOf(sistemasPmsl);
        consertos = consertos == null ? List.of() : List.copyOf(consertos);
    }

    /** Computador e notebook têm softwares; impressora, monitor, telefone e nobreak não. */
    public boolean ehComputador() {
        return "Computador".equals(tipo) || "Notebook".equals(tipo);
    }

    public record Compra(String fornecedor, String notaFiscal, LocalDate data, String valor, String processo, LocalDate garantiaAte) {}

    /** licenca null = sem licença registrada. */
    public record Software(String nome, String licenca, String tipo) {}

    /** Sistemas da prefeitura instalados na máquina, com a linguagem em que foram feitos. */
    public record SistemaPmsl(String nome, String linguagem) {}

    public record Conserto(LocalDate data, String defeito, String solucao, String onde, String custo) {}
}
