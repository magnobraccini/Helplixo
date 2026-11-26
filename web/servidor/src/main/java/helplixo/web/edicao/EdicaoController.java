package helplixo.web.edicao;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modo edição:
 * POST /api/edicao/entrar   {"senha": "..."}  -> {"chave": "...", "validaAte": "..."}
 * POST /api/edicao/sair     (cabeçalho X-Chave-Edicao)
 * GET  /api/edicao          diz se o modo edição está ligado para esta chave
 * As ações de edição mandam a chave no cabeçalho X-Chave-Edicao.
 */
@RestController
@RequestMapping("/api/edicao")
public class EdicaoController {

    public static final String CABECALHO = "X-Chave-Edicao";

    private final EdicaoServico servico;

    public EdicaoController(EdicaoServico servico) {
        this.servico = servico;
    }

    public record PedidoEntrada(String senha) {}

    @PostMapping("/entrar")
    public EdicaoServico.Entrada entrar(@RequestBody PedidoEntrada pedido, HttpServletRequest requisicao) {
        return servico.entrar(pedido.senha(), requisicao.getRemoteAddr());
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@RequestHeader(value = CABECALHO, required = false) String chave) {
        servico.sair(chave);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Map<String, Object> situacao(@RequestHeader(value = CABECALHO, required = false) String chave) {
        Map<String, Object> r = new LinkedHashMap<>();
        Instant ate = servico.validaAte(chave).orElse(null);
        r.put("configurado", servico.configurado());
        r.put("ligado", ate != null);
        r.put("validaAte", ate);
        return r;
    }
}
