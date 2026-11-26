package helplixo.web;

import helplixo.web.edicao.EdicaoServico;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Respostas de erro num formato só: {"erros": ["...", "..."]}.
 * A tela mostra cada item como uma linha da lista de erros.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraVioladaException.class)
    ResponseEntity<Map<String, List<String>>> regra(RegraVioladaException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("erros", e.erros()));
    }

    /** Senha errada: 401 · espere: 429 · modo edição não configurado: 503. */
    @ExceptionHandler(EdicaoServico.EntradaRecusada.class)
    ResponseEntity<Map<String, List<String>>> entradaRecusada(EdicaoServico.EntradaRecusada e) {
        HttpStatus status = switch (e.motivo()) {
            case SENHA_ERRADA -> HttpStatus.UNAUTHORIZED;
            case ESPERAR -> HttpStatus.TOO_MANY_REQUESTS;
            case DESLIGADO -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(Map.of("erros", List.of(e.getMessage())));
    }

    /** Ação de edição sem a chave de edição válida: 403. */
    @ExceptionHandler(EdicaoServico.SemEdicaoException.class)
    ResponseEntity<Map<String, List<String>>> semEdicao(EdicaoServico.SemEdicaoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erros", List.of(e.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, List<String>>> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("erros", List.of("Os dados enviados estão num formato inválido. Confira a data e a hora (ex.: 2026-11-26T10:30).")));
    }
}
