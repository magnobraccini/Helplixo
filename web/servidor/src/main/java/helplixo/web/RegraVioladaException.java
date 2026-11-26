package helplixo.web;

import java.util.List;

/** Um pedido quebrou uma ou mais regras de negócio. A tela mostra a lista de erros ao usuário. */
public class RegraVioladaException extends RuntimeException {

    private final List<String> erros;

    public RegraVioladaException(List<String> erros) {
        super(String.join(" ", erros));
        this.erros = List.copyOf(erros);
    }

    public List<String> erros() {
        return erros;
    }
}
