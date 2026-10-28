package br.com.magnobraccini.helplixo.theme;

import java.awt.Color;

/**
 * Paleta de cores centralizada do Helplixo, alinhada ao tema escuro
 * corporativo já configurado em {@link ThemeManager} (inspirado em
 * Visual Studio / GitHub Desktop / Notion / Windows 11). Componentes devem
 * preferir estas constantes a cores soltas, para manter consistência visual
 * em toda a aplicação.
 */
public final class Palette {
    /** Fundo do header e de superfícies "escuras fixas" (independem do tema claro/escuro ativo). */
    public static final Color SURFACE_DARK = new Color(22, 31, 43);
    public static final Color SURFACE_DARK_ALT = new Color(30, 42, 57);
    public static final Color BORDER_DARK = new Color(56, 72, 91);
    public static final Color TEXT_ON_DARK = new Color(235, 240, 247);
    public static final Color TEXT_ON_DARK_MUTED = new Color(160, 172, 188);

    /** Cor de destaque (accent) usada em toda a aplicação: seleção de abas, botões primários, links. */
    public static final Color ACCENT = new Color(24, 103, 214);
    public static final Color ACCENT_SOFT = new Color(58, 130, 246);

    /** Cores semânticas para estados (sucesso, alerta, perigo, informação). */
    public static final Color SUCCESS = new Color(64, 168, 110);
    public static final Color WARNING = new Color(214, 160, 46);
    public static final Color DANGER = new Color(214, 82, 82);
    public static final Color PURPLE = new Color(139, 106, 246);
    public static final Color TEAL = new Color(38, 176, 173);

    /**
     * Superfícies "elevadas" dos cards modernos do Dashboard (tema escuro corporativo) — um tom
     * levemente mais claro que o fundo da tela ({@code Panel.background} definido em
     * {@link ThemeManager}), para dar sensação de profundidade sem depender de imagens.
     */
    public static final Color DASHBOARD_SURFACE = new Color(29, 40, 54);
    public static final Color DASHBOARD_BORDER = new Color(50, 65, 85);
    public static final Color DASHBOARD_DIVIDER = new Color(42, 55, 72);
    public static final Color DASHBOARD_SHADOW = new Color(0, 0, 0, 70);
    /** Realce translúcido usado na linha selecionada/hover das listas do Dashboard. */
    public static final Color DASHBOARD_SELECTION = new Color(58, 130, 246, 46);

    /**
     * Fundo + texto das caixas semânticas (Informação/Aviso/Observação) do editor de blocos e do
     * documento renderizado — sempre claras, porque a área de conteúdo é sempre clara. Par
     * fundo+texto sempre definido explicitamente junto, para nunca cair em texto claro sobre
     * fundo claro.
     */
    public static final Color INFO_SURFACE = new Color(231, 240, 251);
    public static final Color INFO_SURFACE_TEXT = new Color(24, 52, 93);
    public static final Color WARNING_SURFACE = new Color(253, 243, 222);
    public static final Color WARNING_SURFACE_TEXT = new Color(122, 91, 18);
    public static final Color NOTE_SURFACE = new Color(238, 240, 242);
    public static final Color NOTE_SURFACE_TEXT = new Color(61, 61, 64);

    /** Cor de borda neutra usada em separadores e caixas na área de conteúdo (clara ou escura). */
    public static final Color DIVIDER = new Color(230, 230, 230);

    /**
     * Superfície clara da área de documento (visualização/edição de processo), já usada hoje
     * como {@code Color.WHITE}/valores soltos em ProcessViewerPanel e ProcessEditorPanel — os
     * nomes abaixo apenas centralizam esses mesmos tons na Palette, sem mudar a aparência.
     */
    public static final Color CONTENT_BACKGROUND = Color.WHITE;
    public static final Color CONTENT_SURFACE_ALT = new Color(247, 249, 251);
    public static final Color CONTENT_BORDER = new Color(211, 218, 228);
    public static final Color CONTENT_TEXT_PRIMARY = new Color(28, 37, 49);
    public static final Color CONTENT_TEXT_SECONDARY = new Color(70, 80, 94);

    /** Fundo/texto do card "Aviso"/"Perigo" na área de conteúdo clara (par explícito fundo+texto). */
    public static final Color DANGER_SURFACE = new Color(252, 233, 233);
    public static final Color DANGER_SURFACE_TEXT = new Color(148, 39, 39);
    public static final Color SUCCESS_SURFACE = new Color(228, 245, 235);
    public static final Color SUCCESS_SURFACE_TEXT = new Color(31, 110, 66);

    private Palette() {
    }
}
