package br.com.magnobraccini.helplixo.view.settings;

import br.com.magnobraccini.helplixo.core.Constants;
import br.com.magnobraccini.helplixo.core.Version;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Tela de Configurações, acessada pela engrenagem. Reúne Idioma,
 * Segurança (senha de edição), Backup, Local dos dados, Importar/Exportar
 * e Sobre — cada uma em uma caixa própria.
 *
 * <p>Por segurança, a senha para entrar em modo de edição é sempre exigida — não existe mais um
 * botão para desativá-la, nem aqui nem em {@code SecurityService}. Pelo mesmo motivo, o caminho
 * completo da pasta de dados não é mostrado nesta tela (evita expor o usuário do Windows em
 * capturas de tela); o botão "Abrir pasta" continua funcionando normalmente.</p>
 */
public final class SettingsPanel extends JPanel {

    private final JButton changePasswordButton = new JButton("Alterar senha...");
    private final JButton backupNowButton = new JButton("Salvar uma cópia (versão) em outra pasta...");
    private final JButton openFolderButton = new JButton("Abrir pasta");
    private final JButton exportButton = new JButton("Exportar dados...");
    private final JButton importButton = new JButton("Importar dados...");

    public SettingsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Configurações");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        add(title, BorderLayout.NORTH);

        JPanel stacked = new JPanel();
        stacked.setLayout(new BoxLayout(stacked, BoxLayout.Y_AXIS));

        stacked.add(card("Idioma", languageSection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Atalhos de teclado", shortcutsSection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Segurança", securitySection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Backup", backupSection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Local dos dados", dataLocationSection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Importar / Exportar", importExportSection()));
        stacked.add(Box.createVerticalStrut(12));
        stacked.add(card("Sobre", aboutSection()));

        JScrollPane scroll = new JScrollPane(stacked);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel card(String titleText, JPanel content) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panel.getForeground(), 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, panel.getMaximumSize().height));

        JLabel label = new JLabel(titleText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel languageSection() {
        JLabel languageInfo = new JLabel("Português (Brasil) — único idioma disponível no momento.");
        languageInfo.setEnabled(false);
        return rows(row("Idioma", languageInfo));
    }

    private JPanel shortcutsSection() {
        return rows(
                row("Pesquisar", shortcutLabel("Ctrl+K")),
                row("Novo processo", shortcutLabel("Ctrl+N")),
                row("Editar processo", shortcutLabel("Ctrl+E")),
                row("Salvar", shortcutLabel("Ctrl+S")),
                row("Desfazer (no editor)", shortcutLabel("Ctrl+Z")),
                row("Refazer (no editor)", shortcutLabel("Ctrl+Y")),
                row("Tecla enviada pelo Autoclicker", shortcutLabel("F6")));
    }

    private JLabel shortcutLabel(String shortcut) {
        JLabel label = new JLabel(shortcut);
        label.setEnabled(false);
        return label;
    }

    private JPanel securitySection() {
        JLabel info = new JLabel("Por segurança, a senha é sempre exigida para entrar em modo de edição.");
        info.setEnabled(false);
        return rows(single(info), single(changePasswordButton));
    }

    private JPanel backupSection() {
        JLabel info = new JLabel("Salva uma cópia (versão) de todos os dados na pasta que você escolher.");
        info.setEnabled(false);
        return rows(single(info), single(backupNowButton));
    }

    private JPanel dataLocationSection() {
        JLabel info = new JLabel("Por segurança, o caminho da pasta não é exibido aqui. Use o botão para abri-la.");
        info.setEnabled(false);
        return rows(single(info), single(openFolderButton));
    }

    private JPanel importExportSection() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(exportButton);
        buttons.add(importButton);
        return rows(single(buttons));
    }

    private JPanel aboutSection() {
        JLabel about = new JLabel("<html>" + Constants.APP_NAME + " — versão " + Version.CURRENT
                + "<br>Criado por: Magno Braccini</html>");
        return rows(single(about));
    }

    // --- Pequeno helper de layout: empilha "linhas" (label+campo ou só um componente) em um painel. ---

    private record FieldRow(String label, Component component) {
    }

    private FieldRow row(String label, Component component) {
        return new FieldRow(label, component);
    }

    private FieldRow single(Component component) {
        return new FieldRow(null, component);
    }

    private JPanel rows(FieldRow... fieldRows) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        int y = 0;
        for (FieldRow fieldRow : fieldRows) {
            c.gridy = y++;
            c.gridx = 0;
            if (fieldRow.label() != null) {
                c.weightx = 0;
                c.gridwidth = 1;
                panel.add(new JLabel(fieldRow.label()), c);
                c.gridx = 1;
            } else {
                c.gridwidth = 2;
            }
            c.weightx = 1;
            panel.add(fieldRow.component(), c);
        }
        return panel;
    }

    public void onChangePassword(Runnable listener) {
        changePasswordButton.addActionListener(event -> listener.run());
    }

    public void onBackupNow(Runnable listener) {
        backupNowButton.addActionListener(event -> listener.run());
    }

    public void onOpenDataFolder(Runnable listener) {
        openFolderButton.addActionListener(event -> listener.run());
    }

    public void onExport(Runnable listener) {
        exportButton.addActionListener(event -> listener.run());
    }

    public void onImport(Runnable listener) {
        importButton.addActionListener(event -> listener.run());
    }
}

