package helplixo.web.chamado;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Os três status de um chamado. No JSON aparecem como na tela: "Aberto", "Em andamento", "Fechado". */
public enum StatusChamado {
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    FECHADO("Fechado");

    private final String rotulo;

    StatusChamado(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    /** Aceita o rótulo da tela ("Em andamento") ou o nome da constante ("EM_ANDAMENTO"). */
    @JsonCreator
    public static StatusChamado de(String texto) {
        for (StatusChamado s : values()) {
            if (s.rotulo.equalsIgnoreCase(texto) || s.name().equalsIgnoreCase(texto)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Status inválido: " + texto + ". Use Aberto, Em andamento ou Fechado.");
    }
}
