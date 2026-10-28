package br.com.magnobraccini.helplixo.component;

import br.com.magnobraccini.helplixo.theme.Palette;
import br.com.magnobraccini.helplixo.util.ClipboardFiles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * Card "Arquivos / Links" do painel lateral direito de um processo. Mostra exatamente os itens
 * reais de {@code ProcessDocument.getLinks()} — nenhum tamanho de arquivo é exibido porque essa
 * informação não existe no modelo hoje; inventar um valor de exemplo violaria a regra de não
 * apresentar informação falsa. Cada item pode ser aberto no programa padrão do sistema ("↗") e
 * copiado ("📋"): se for um arquivo local anexado, copia o arquivo em si (Ctrl+V cola o arquivo,
 * igual ao Explorer); se for um link, copia o texto do link.
 */
public final class FilesLinksCard extends JPanel {
    private final JPanel list = new JPanel();
    private final JLabel emptyLabel = new JLabel("Nenhum arquivo ou link cadastrado para este processo.");

    public FilesLinksCard() {
        setLayout(new BorderLayout(0, 8));
        setOpaque(true);
        setBackground(Palette.CONTENT_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.CONTENT_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("Arquivos / Links");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
        title.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        list.setLayout(new GridLayout(0, 1, 0, 6));
        list.setOpaque(false);
        emptyLabel.setForeground(Palette.CONTENT_TEXT_SECONDARY);
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 12f));
        add(list, BorderLayout.CENTER);
    }

    public void render(List<String> links) {
        list.removeAll();
        if (links == null || links.isEmpty()) {
            list.setLayout(new BorderLayout());
            list.add(emptyLabel, BorderLayout.NORTH);
        } else {
            list.setLayout(new GridLayout(0, 1, 0, 6));
            links.forEach(link -> list.add(row(link)));
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel row(String link) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel tag = new JLabel(tagFor(link));
        tag.setFont(tag.getFont().deriveFont(Font.BOLD, 10f));
        tag.setForeground(Palette.ACCENT);

        JLabel text = new JLabel(link);
        text.setForeground(Palette.CONTENT_TEXT_PRIMARY);
        text.setFont(text.getFont().deriveFont(Font.PLAIN, 12f));

        JPanel labelGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        labelGroup.setOpaque(false);
        labelGroup.add(tag);
        labelGroup.add(text);

        JButton copy = new JButton("📋");
        copy.setToolTipText("Copiar");
        copy.setFocusable(false);
        copy.putClientProperty("JButton.buttonType", "toolBarButton");
        copy.addActionListener(event -> copyLink(link));

        JButton open = new JButton("↗");
        open.setToolTipText("Abrir");
        open.setFocusable(false);
        open.putClientProperty("JButton.buttonType", "toolBarButton");
        open.addActionListener(event -> openLink(link));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        actions.setOpaque(false);
        actions.add(copy);
        actions.add(open);

        row.add(labelGroup, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void openLink(String link) {
        File file = asExistingFile(link);
        try {
            if (file != null) {
                Desktop.getDesktop().open(file);
            } else {
                String target = link.contains("://") ? link : "https://" + link;
                Desktop.getDesktop().browse(new URI(target));
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Não foi possível abrir este item automaticamente:\n" + link,
                    "Arquivos / Links", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Copia o item para a área de transferência: se for um arquivo anexado que ainda existe no
     * computador, copia o arquivo em si (como Ctrl+C no Explorer); senão, copia o texto do link.
     */
    private void copyLink(String link) {
        File file = asExistingFile(link);
        try {
            if (file != null) {
                ClipboardFiles.copyFile(file);
            } else {
                ClipboardFiles.copyText(link);
            }
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                    "Não foi possível copiar este item:\n" + link,
                    "Arquivos / Links", JOptionPane.WARNING_MESSAGE);
        }
    }

    private File asExistingFile(String value) {
        if (value == null || value.isBlank() || value.contains("://")) {
            return null;
        }
        File file = new File(value);
        return file.isFile() ? file : null;
    }

    private String tagFor(String link) {
        String lower = link.toLowerCase();
        int dot = lower.lastIndexOf('.');
        if (dot >= 0 && dot > lower.length() - 6) {
            String extension = lower.substring(dot + 1).replaceAll("[^a-z0-9]", "");
            if (!extension.isBlank() && extension.length() <= 4) {
                return "[" + extension.toUpperCase() + "]";
            }
        }
        return "[LINK]";
    }
}

