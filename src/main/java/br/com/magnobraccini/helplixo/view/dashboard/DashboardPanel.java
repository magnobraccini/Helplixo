package br.com.magnobraccini.helplixo.view.dashboard;

import br.com.magnobraccini.helplixo.component.Card;
import br.com.magnobraccini.helplixo.component.IconBadge;
import br.com.magnobraccini.helplixo.model.DashboardSummary;
import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.model.Notice;
import br.com.magnobraccini.helplixo.model.PhoneContact;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.model.Tool;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dashboard: três cards modernos lado a lado — Recentes, Avisos e Favoritos — mantendo a mesma
 * disposição de sempre. O gráfico de "Avisos por categoria" que ficava no topo foi removido a
 * pedido do usuário; o espaço liberado foi usado para dar mais respiro ao cabeçalho e aos cards.
 * Clicar num aviso abre uma caixa de detalhe com quem publicou, data, hora e o texto do aviso.
 */
public final class DashboardPanel extends JPanel {
    private final DefaultListModel<Object> noticesModel = new DefaultListModel<>();
    private final JList<Object> noticesList = new JList<>(noticesModel);
    private final Card noticesCard = new Card("◇", Palette.DANGER, "Avisos");

    private final DefaultListModel<ProcessDocument> recentModel = new DefaultListModel<>();
    private final JList<ProcessDocument> recentList = new JList<>(recentModel);
    private final Card recentCard = new Card("↻", Palette.ACCENT_SOFT, "Recentes");

    private final DefaultListModel<Object> favoritesModel = new DefaultListModel<>();
    private final JList<Object> favoritesList = new JList<>(favoritesModel);
    private final Card favoritesCard = new Card("★", Palette.WARNING, "Favoritos");

    private Consumer<ProcessDocument> openRecentListener = ignored -> { };
    private Consumer<ProcessDocument> openFavoriteProcessListener = ignored -> { };
    private Consumer<Folder> openFavoriteFolderListener = ignored -> { };
    private Consumer<PhoneContact> openFavoritePhoneContactListener = ignored -> { };
    private Consumer<Notice> openFavoriteNoticeListener = ignored -> { };
    private Consumer<Tool> openFavoriteToolListener = ignored -> { };
    private Consumer<Notice> openNoticeListener = ignored -> { };

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setForeground(Palette.TEXT_ON_DARK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        add(title, BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 3, 18, 0));
        columns.setOpaque(false);
        columns.add(buildRecentBox());
        columns.add(buildNoticesBox());
        columns.add(buildFavoritesBox());
        add(columns, BorderLayout.CENTER);
    }

    public void onOpenRecent(Consumer<ProcessDocument> listener) {
        openRecentListener = listener == null ? ignored -> { } : listener;
    }

    public void onOpenFavoriteProcess(Consumer<ProcessDocument> listener) {
        openFavoriteProcessListener = listener == null ? ignored -> { } : listener;
    }

    public void onOpenFavoriteFolder(Consumer<Folder> listener) {
        openFavoriteFolderListener = listener == null ? ignored -> { } : listener;
    }

    public void onOpenFavoritePhoneContact(Consumer<PhoneContact> listener) {
        openFavoritePhoneContactListener = listener == null ? ignored -> { } : listener;
    }

    public void onOpenFavoriteNotice(Consumer<Notice> listener) {
        openFavoriteNoticeListener = listener == null ? ignored -> { } : listener;
    }

    public void onOpenFavoriteTool(Consumer<Tool> listener) {
        openFavoriteToolListener = listener == null ? ignored -> { } : listener;
    }

    /** Disparado quando o usuário escolhe "Abrir em Avisos" na caixa de detalhe do aviso. */
    public void onOpenNotice(Consumer<Notice> listener) {
        openNoticeListener = listener == null ? ignored -> { } : listener;
    }

    public void render(DashboardSummary summary, List<ProcessDocument> recentProcesses, List<Object> favorites,
                        List<Object> notices) {
        recentModel.clear();
        recentProcesses.forEach(recentModel::addElement);
        recentCard.setCount(recentProcesses.size());

        favoritesModel.clear();
        favorites.forEach(favoritesModel::addElement);
        favoritesCard.setCount(favorites.size());

        noticesModel.clear();
        if (notices.isEmpty()) {
            noticesModel.addElement("Nenhum aviso cadastrado no momento.");
            noticesList.setEnabled(false);
        } else {
            notices.forEach(noticesModel::addElement);
            noticesList.setEnabled(true);
        }
        noticesCard.setCount(notices.size());
    }

    private Card buildNoticesBox() {
        noticesModel.addElement("Nenhum aviso cadastrado no momento.");
        noticesList.setEnabled(false);
        noticesList.setCellRenderer(new NoticeCellRenderer());
        noticesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (noticesList.getSelectedValue() instanceof Notice notice) {
                    showNoticeDetail(notice);
                }
            }
        });
        styleList(noticesList);
        noticesCard.getBody().add(scrollable(noticesList), BorderLayout.CENTER);
        return noticesCard;
    }

    private void showNoticeDetail(Notice notice) {
        br.com.magnobraccini.helplixo.view.dialogs.NoticeDetailDialog.show(this, notice,
                () -> openNoticeListener.accept(notice));
    }

    private Card buildRecentBox() {
        recentList.setCellRenderer(new RecentCellRenderer());
        recentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && recentList.getSelectedValue() != null) {
                    openRecentListener.accept(recentList.getSelectedValue());
                }
            }
        });
        styleList(recentList);
        recentCard.getBody().add(scrollable(recentList), BorderLayout.CENTER);
        return recentCard;
    }

    private Card buildFavoritesBox() {
        favoritesList.setCellRenderer(new FavoriteCellRenderer());
        favoritesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2) {
                    return;
                }
                Object selected = favoritesList.getSelectedValue();
                if (selected instanceof ProcessDocument process) {
                    openFavoriteProcessListener.accept(process);
                } else if (selected instanceof Folder folder) {
                    openFavoriteFolderListener.accept(folder);
                } else if (selected instanceof PhoneContact contact) {
                    openFavoritePhoneContactListener.accept(contact);
                } else if (selected instanceof Notice notice) {
                    openFavoriteNoticeListener.accept(notice);
                } else if (selected instanceof Tool tool) {
                    openFavoriteToolListener.accept(tool);
                }
            }
        });
        styleList(favoritesList);
        favoritesCard.getBody().add(scrollable(favoritesList), BorderLayout.CENTER);
        return favoritesCard;
    }

    private void styleList(JList<?> list) {
        list.setOpaque(false);
        list.setBackground(new Color(0, 0, 0, 0));
        list.setForeground(Palette.TEXT_ON_DARK);
        list.setSelectionBackground(new Color(0, 0, 0, 0));
        list.setSelectionForeground(Palette.TEXT_ON_DARK);
        list.setFixedCellHeight(-1);
        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private JScrollPane scrollable(JList<?> list) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /** Linha de lista reutilizável: emblema colorido + rótulo pequeno opcional + texto principal. */
    private static final class ModernRow extends JPanel {
        private final IconBadge badge;
        private final JLabel captionLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private boolean selected;

        ModernRow(boolean withCaption) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BorderLayout(12, 0));
            badge = new IconBadge("", Palette.ACCENT_SOFT, 30);
            add(badge, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
            captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD, 10.5f));
            captionLabel.setVisible(withCaption);
            titleLabel.setForeground(Palette.TEXT_ON_DARK);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 13f));
            if (withCaption) {
                text.add(captionLabel);
                text.add(javax.swing.Box.createVerticalStrut(2));
            }
            text.add(titleLabel);
            add(text, BorderLayout.CENTER);
        }

        void set(String glyph, Color color, String caption, String title, boolean isSelected) {
            badge.setGlyph(glyph);
            badge.setBadgeColor(color);
            captionLabel.setText(caption);
            captionLabel.setForeground(color);
            titleLabel.setText(title);
            this.selected = isSelected;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Palette.DASHBOARD_SELECTION);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static JLabel emptyLabel(Object value) {
        JLabel label = new JLabel(String.valueOf(value));
        label.setForeground(Palette.TEXT_ON_DARK_MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 12.5f));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        return label;
    }

    private static final class RecentCellRenderer implements ListCellRenderer<ProcessDocument> {
        private final ModernRow row = new ModernRow(false);

        @Override
        public Component getListCellRendererComponent(JList<? extends ProcessDocument> list, ProcessDocument value,
                                                        int index, boolean isSelected, boolean cellHasFocus) {
            row.set("📄", Palette.ACCENT_SOFT, "", value.getTitle(), isSelected);
            return row;
        }
    }

    private static final class NoticeCellRenderer implements ListCellRenderer<Object> {
        private final ModernRow row = new ModernRow(true);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                        boolean cellHasFocus) {
            if (value instanceof Notice notice) {
                Color color = categoryColor(notice.getCategory());
                String category = notice.getCategory() == null ? "" : notice.getCategory().getDisplayName().toUpperCase();
                row.set("◇", color, category, notice.getTitle(), isSelected);
                return row;
            }
            return emptyLabel(value);
        }

        private Color categoryColor(br.com.magnobraccini.helplixo.model.NoticeCategory category) {
            if (category == null) {
                return Palette.ACCENT_SOFT;
            }
            return switch (category) {
                case URGENTE -> Palette.DANGER;
                case REDE -> Palette.ACCENT_SOFT;
                case INFRAESTRUTURA -> Palette.PURPLE;
                case SISTEMA -> Palette.TEAL;
                case MANUTENCAO -> Palette.WARNING;
            };
        }
    }

    private static final class FavoriteCellRenderer implements ListCellRenderer<Object> {
        private final ModernRow row = new ModernRow(false);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                        boolean cellHasFocus) {
            String glyph;
            Color color;
            String text;
            switch (value) {
                case Folder folder -> {
                    glyph = "📁";
                    color = Palette.WARNING;
                    text = folder.getName();
                }
                case ProcessDocument process -> {
                    glyph = "📄";
                    color = Palette.ACCENT_SOFT;
                    text = process.getTitle();
                }
                case PhoneContact contact -> {
                    glyph = "☎";
                    color = Palette.SUCCESS;
                    text = contact.getName();
                }
                case Notice notice -> {
                    glyph = "◇";
                    color = Palette.DANGER;
                    text = notice.getTitle();
                }
                case Tool tool -> {
                    glyph = tool.getIcon();
                    color = Palette.TEAL;
                    text = tool.getName();
                }
                default -> {
                    glyph = "•";
                    color = Palette.ACCENT_SOFT;
                    text = String.valueOf(value);
                }
            }
            row.set(glyph, color, "", text, isSelected);
            return row;
        }
    }
}
