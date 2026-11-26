package helplixo.web.inventario;

import java.time.LocalDateTime;

/**
 * Uma alteração no cadastro da máquina, para o histórico.
 * tipo: Cadastro (máquina criada ou editada) ou Baixa.
 * texto: o que mudou, ex.: "RAM: 16 GB DDR4 → 32 GB DDR4 · Local: Informática - SMED → Depósito da TI".
 */
public record RegistroCadastro(String nSemin, LocalDateTime quando, String tipo, String texto, String quem) {}
