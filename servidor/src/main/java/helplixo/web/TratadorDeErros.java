package helplixo.web;

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, List<String>>> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("erros", List.of("Os dados enviados estão num formato inválido. Confira a data e a hora (ex.: 2026-11-26T10:30).")));
    }
}
