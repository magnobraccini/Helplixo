package helplixo.web;

import java.util.Map;

/**
 * Cargos: cada um tem um nível e um setor. Quem tem nível maior no mesmo setor tem poder sobre o outro
 * (hoje, só para remover avisos do calendário). Até existir o login, o cargo chega no cabeçalho X-Cargo.
 */
public final class Cargos {

    public static final String CABECALHO = "X-Cargo";

    public record Cargo(int nivel, String setor) {}

    private static final Map<String, Cargo> CARGOS = Map.of(
            "Triagem", new Cargo(1, "Informática"),
            "Técnico", new Cargo(1, "Informática"),
            "Chefe de TI", new Cargo(3, "Informática"));

    private Cargos() {}

    public static boolean existe(String cargo) {
        return cargo != null && CARGOS.containsKey(cargo);
    }

    /** O cargo A pode agir sobre algo do cargo B? (o mesmo cargo, ou nível maior no mesmo setor) */
    public static boolean podeSobre(String cargoA, String cargoB) {
        Cargo a = CARGOS.get(cargoA), b = CARGOS.get(cargoB);
        if (a == null || b == null) return false;
        if (cargoA.equals(cargoB)) return true;
        return a.nivel() > b.nivel() && a.setor().equals(b.setor());
    }
}
