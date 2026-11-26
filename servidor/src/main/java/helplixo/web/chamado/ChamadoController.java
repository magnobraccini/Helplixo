package helplixo.web.chamado;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de chamados.
 * GET /api/chamados?de=2026-11-01&ate=2026-11-30&status=Aberto
 * Sem datas, devolve os últimos 30 dias (como a tela abre).
 */
@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoRepositorio repositorio;
    private final Clock relogio;

    public ChamadoController(ChamadoRepositorio repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    @GetMapping
    public List<Chamado> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String status) {
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate fim = ate != null ? ate : hoje;
        LocalDate inicio = de != null ? de : fim.minusDays(30);
        if (inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial (de) precisa ser antes da final (ate).");
        }
        StatusChamado filtro;
        try {
            filtro = (status == null || status.isBlank() || status.equalsIgnoreCase("Todos")) ? null : StatusChamado.de(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        // "ate" inclui o dia inteiro
        return repositorio.buscar(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay(), filtro);
    }

    @GetMapping("/{numero}")
    public Chamado detalhe(@PathVariable String numero) {
        return repositorio.porNumero(numero)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado " + numero + " não encontrado."));
    }
}
