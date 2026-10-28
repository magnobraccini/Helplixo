package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.core.Version;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Rodapé da aplicação. Mostra, da esquerda para a direita: total de itens (dado real,
 * via {@link #setTotalItems(long)}) seguido da mensagem dinâmica de feedback das ações
 * do usuário (ex.: "Processo salvo com sucesso"); ao centro, o nome e a versão real do
 * Helplixo ({@link Version#CURRENT}) — o crédito ao autor fica como tooltip, para não
 * disputar espaço com o restante; à direita, um atalho para Configurações que reaproveita
 * a navegação já existente (não é uma tela nova).
 */
public final class StatusBar extends JPanel {
    private static final Color BACKGROUND = Palette.SURFACE_DARK;

    private final JLabel totalItemsLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel("Pronto");
    private final JLabel settingsLink = new JLabel("Configurações");
    private Runnable openSettingsListener = () -> { };

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.BORDER_DARK),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        totalItemsLabel.setForeground(Palette.TEXT_ON_DARK_MUTED);
        messageLabel.setForeground(Palette.TEXT_ON_DARK_MUTED);
        left.add(totalItemsLabel);
        left.add(messageLabel);
        add(left, BorderLayout.WEST);

        JLabel brand = new JLabel(Constants.APP_NAME + " " + Version.CURRENT);
        brand.setForeground(Palette.TEXT_ON_DARK_MUTED);
        brand.setFont(brand.getFont().deriveFont(Font.PLAIN, 12f));
        brand.setHorizontalAlignment(SwingConstants.CENTER);
        brand.setToolTipText("Criado por: Magno Braccini");
        add(brand, BorderLayout.CENTER);

        settingsLink.setForeground(Palette.TEXT_ON_DARK_MUTED);
        settingsLink.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        settingsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                openSettingsListener.run();
            }
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(settingsLink);
        add(right, BorderLayout.EAST);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /** Total de itens reais (ex.: processos) exibido no rodapé — nunca um valor fixo. */
    public void setTotalItems(long total) {
        totalItemsLabel.setText("Total: " + total + (total == 1 ? " item" : " itens"));
    }

    /** Chamado ao clicar em "Configurações" no rodapé; reaproveita a navegação já existente. */
    public void onOpenSettings(Runnable listener) {
        openSettingsListener = listener == null ? () -> { } : listener;
    }

    public void setEditMode(boolean editMode) {
        // O indicador de modo (edição/visualização) já é comunicado pelo botão
        // "Ativar edição"/"Edição ativa" no header e pelos botões exclusivos de
        // edição na toolbar de Processos — mantido aqui apenas por compatibilidade de chamada.
    }
}
