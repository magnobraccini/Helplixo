package helplixo.web.calendario;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Uma marcação do calendário.
 * - horaInicio null = dia inteiro.
 * - fim = último dia de um período (férias); só vale sem repetição.
 * - repete: "" (não), "semanal", "mensal" ou "anual"; repeteAte = data para terminar (null = nunca).
 * - lembrete: "" (sem), "dia" (começo do dia, 7h30), "30", "60" ou "1440" minutos antes.
 * - automatica = feriado, contrato ou garantia: entra sozinha e não pode ser removida.
 */
public record Marcacao(
        String id,
        String titulo,
        String tipo,
        LocalDate inicio,
        LocalDate fim,
        LocalTime horaInicio,
        LocalTime horaFim,
        String repete,
        LocalDate repeteAte,
        String lembrete,
        String descricao,
        String autor,
        String cargo,
        boolean urgente,
        boolean automatica) {

    public Marcacao {
        repete = repete == null ? "" : repete;
        lembrete = lembrete == null ? "" : lembrete;
    }

    /** Esta marcação cai neste dia? Considera períodos e repetições. */
    public boolean caiEm(LocalDate dia) {
        if (dia.isBefore(inicio)) return false;
        if (repete.isEmpty()) return !dia.isAfter(fim == null ? inicio : fim);
        if (repeteAte != null && dia.isAfter(repeteAte)) return false;
        return switch (repete) {
            case "semanal" -> dia.getDayOfWeek() == inicio.getDayOfWeek();
            case "mensal" -> dia.getDayOfMonth() == inicio.getDayOfMonth();
            case "anual" -> dia.getMonth() == inicio.getMonth() && dia.getDayOfMonth() == inicio.getDayOfMonth();
            default -> false;
        };
    }

    /** Ex.: "Toda semana (quinta-feira), até 31/12/2026". */
    public String repeteTexto() {
        String base = switch (repete) {
            case "semanal" -> "Toda semana (" + diaDaSemana(inicio.getDayOfWeek()) + ")";
            case "mensal" -> "Todo mês, no dia " + inicio.getDayOfMonth();
            case "anual" -> String.format("Todo ano, em %02d/%02d", inicio.getDayOfMonth(), inicio.getMonthValue());
            default -> "";
        };
        if (base.isEmpty() || repeteAte == null) return base;
        return base + String.format(", até %02d/%02d/%d", repeteAte.getDayOfMonth(), repeteAte.getMonthValue(), repeteAte.getYear());
    }

    private static String diaDaSemana(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "segunda-feira";
            case TUESDAY -> "terça-feira";
            case WEDNESDAY -> "quarta-feira";
            case THURSDAY -> "quinta-feira";
            case FRIDAY -> "sexta-feira";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
