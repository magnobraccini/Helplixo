package helplixo.web.inventario;

import java.time.LocalDateTime;

/** A máquina saiu de um lugar e foi para outro. chamado = número do chamado ligado (pode ser vazio). */
public record Movimentacao(String nSemin, String de, String para, String motivo, LocalDateTime quando, String tecnico, String chamado) {}
