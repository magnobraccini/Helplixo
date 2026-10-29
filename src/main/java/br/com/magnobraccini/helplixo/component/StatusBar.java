package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.core.Version;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Rodapé compacto do aplicativo: mensagem de estado à esquerda e uma identificação central
 * que também deve ser revisada quando a tela principal mudar. A navegação para Configurações
 * permanece na engrenagem do topo.
 */
public final class StatusBar extends JPanel {
    private static final Color BACKGROUND = Palette.SURFACE_DARK;

    private final JLabel messageLabel = new JLabel("Pronto");

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Palette.BORDER_DARK),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));

        messageLabel.setForeground(Palette.TEXT_ON_DARK_MUTED);
        add(messageLabel, BorderLayout.WEST);

        // Atualize esta identificação sempre que a tela principal ganhar uma nova área principal.
        JLabel brand = new JLabel(Constants.APP_NAME + " " + Version.CURRENT + " · Processos e procedimentos");
        brand.setForeground(Palette.TEXT_ON_DARK_MUTED);
        brand.setFont(brand.getFont().deriveFont(Font.PLAIN, 12f));
        brand.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        add(brand, BorderLayout.CENTER);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setEditMode(boolean editMode) {
        // O indicador de modo (edição/visualização) já é comunicado pelo botão
        // "Ativar edição"/"Edição ativa" no header e pelos botões exclusivos de
        // edição na toolbar de Processos — mantido aqui apenas por compatibilidade de chamada.
    }
}
