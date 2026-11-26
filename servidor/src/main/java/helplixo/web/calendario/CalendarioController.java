package helplixo.web.calendario;

import helplixo.web.Cargos;
import helplixo.web.edicao.EdicaoController;
import helplixo.web.inventario.InventarioController;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Calendário:
 * GET    /api/calendario?de=2026-11-01&ate=2026-11-30&tipos=Aviso,Reunião&q=backup   ocorrências dia a dia
 * POST   /api/calendario            marcar (Aviso: qualquer cargo; o resto: modo edição)
 * DELETE /api/calendario/{id}       remover (autor ou cargo acima no mesmo setor)
 * GET    /api/calendario/lembretes  lembretes que já devem aparecer no sino
 * Quem marca: cabeçalhos X-Usuario e X-Cargo, até existir o login.
 */
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final CalendarioServico servico;

    public CalendarioController(CalendarioServico servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<CalendarioServico.Ocorrencia> ocorrencias(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String tipos,
            @RequestParam(required = false) String q) {
        Set<String> filtro = tipos == null || tipos.isBlank() ? Set.of()
                : Arrays.stream(tipos.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        return servico.ocorrencias(de, ate, filtro, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarioServico.Criada marcar(@RequestBody CalendarioServico.PedidoMarcacao pedido,
                                          @RequestHeader(value = EdicaoController.CABECALHO, required = false) String chave,
                                          @RequestHeader(value = InventarioController.CABECALHO_USUARIO, required = false) String usuario,
                                          @RequestHeader(value = Cargos.CABECALHO, required = false) String cargo) {
        return servico.criar(pedido, chave, usuario, cargo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable String id, @RequestHeader(value = Cargos.CABECALHO, required = false) String cargo) {
        servico.remover(id, cargo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lembretes")
    public List<CalendarioServico.Lembrete> lembretes() {
        return servico.lembretes();
    }
}
