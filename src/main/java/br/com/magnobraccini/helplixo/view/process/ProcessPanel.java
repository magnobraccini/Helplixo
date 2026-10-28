package br.com.magnobraccini.helplixo.view.process;

import br.com.magnobraccini.helplixo.model.Folder;
import br.com.magnobraccini.helplixo.model.ProcessDocument;
import br.com.magnobraccini.helplixo.theme.Palette;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ProcessPanel extends JPanel {
    private static final com.formdev.flatlaf.extras.FlatSVGIcon STAR_FILLED_ICON =
            new com.formdev.flatlaf.extras.FlatSVGIcon("icons/svg/star-filled.svg");
    private static final com.formdev.flatlaf.extras.FlatSVGIcon STAR_OUTLINE_ICON =
            new com.formdev.flatlaf.extras.FlatSVGIcon("icons/svg/star-outline.svg");

    private static final String VIEW_CARD = "view";
    private static final String EDIT_CARD = "edit";

    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Navegação");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    private final JTree tree = new JTree(treeModel);
    private final java.util.Set<Object> expandedKeys = new java.util.HashSet<>();
    private int hoveredRow = -1;
    private final Map<UUID, DefaultMutableTreeNode> folderNodes = new HashMap<>();
    private final Map<UUID, DefaultMutableTreeNode> processNodes = new HashMap<>();
    private final ProcessViewerPanel viewerPanel = new ProcessViewerPanel();
    private final ProcessEditorPanel editorPanel = new ProcessEditorPanel();
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(contentLayout);
    private final JPanel breadcrumbBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JButton favoriteButton = new JButton(STAR_OUTLINE_ICON);
    private final JLabel unsavedLabel = new JLabel(" ");

    private final JButton newFolderButton = button("＋ Nova seção");
    private final JButton newProcessButton = button("＋ Novo processo");
    private final JButton editButton = button("✎ Editar");
    private final JButton deleteButton = button("🗑 Excluir");
    private final JButton moreButton = button("⋮ Mais");
    private final JMenuItem renameMenuItem = new JMenuItem("Renomear");
    private final JMenuItem moveMenuItem = new JMenuItem("Mover para seção");
    private final JMenuItem refreshMenuItem = new JMenuItem("Atualizar");
    private final JButton undoButton = button("↶ Desfazer");
    private final JButton redoButton = button("↷ Refazer");
    private final JButton saveButton = button("Salvar");
    private final JButton cancelButton = button("Cancelar");

    private Consumer<Object> selectionListener = ignored -> { };
    private Runnable favoriteListener = () -> { };
    private boolean editAuthorized;
    private boolean editing;
    private boolean dirty;
    private UUID selectedFolderId;
    private UUID selectedProcessId;
    private Object selectedObject;

    public ProcessPanel() {
        setLayout(new BorderLayout());
        add(createToolbar(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        editorPanel.onChange(this::markDirty);
        undoButton.addActionListener(event -> { editorPanel.undo(); updateActionState(); });
        redoButton.addActionListener(event -> { editorPanel.redo(); updateActionState(); });
        viewerPanel.onSwitchToEdit(() -> { if (editButton.isEnabled()) editButton.doClick(); });
        editorPanel.onSwitchToView(() -> { if (cancelButton.isEnabled()) cancelButton.doClick(); });
        installShortcuts();
        viewerPanel.renderWelcome();
        renderBreadcrumb(List.of(new BreadcrumbSegment("Processos", null)));
        setFavoriteState(false, false);
        updateActionState();
    }

    public void renderTree(List<Folder> folders,
                           Map<UUID, List<ProcessDocument>> processesByFolder,
                           List<Folder> favoriteFolders,
                           List<ProcessDocument> favoriteProcesses,
                           List<ProcessDocument> recentProcesses) {
        rootNode.removeAllChildren();
        folderNodes.clear();
        processNodes.clear();

        rootNode.add(createFavoritesNode(favoriteFolders, favoriteProcesses));
        rootNode.add(createRecentNode(recentProcesses));

        folders.forEach(folder -> folderNodes.put(folder.getId(), new DefaultMutableTreeNode(folder)));
        folders.forEach(folder -> {
            DefaultMutableTreeNode node = folderNodes.get(folder.getId());
            DefaultMutableTreeNode parent = folder.getParentId() == null
                    ? rootNode : folderNodes.getOrDefault(folder.getParentId(), rootNode);
            parent.add(node);
            processesByFolder.getOrDefault(folder.getId(), List.of()).forEach(process -> {
                DefaultMutableTreeNode processNode = new DefaultMutableTreeNode(process);
                node.add(processNode);
                processNodes.put(process.getId(), processNode);
            });
        });

        treeModel.reload();
        restoreExpandedState();
        restoreSelection();
    }

    public void renderProcess(ProcessDocument process, List<BreadcrumbSegment> breadcrumb) {
        selectedProcessId = process.getId();
        selectedFolderId = process.getFolderId();
        selectedObject = process;
        viewerPanel.render(process);
        renderBreadcrumb(breadcrumb);
        setFavoriteState(process.isFavorite(), true);
        editing = false;
        dirty = false;
        unsavedLabel.setText(" ");
        contentLayout.show(contentCards, VIEW_CARD);
        updateActionState();
    }

    public void renderFolder(Folder folder, List<BreadcrumbSegment> breadcrumb) {
        selectedFolderId = folder.getId();
        selectedProcessId = null;
        selectedObject = folder;
        viewerPanel.renderFolder(folder.getName(), folder.getDescription(), true);
        renderBreadcrumb(breadcrumb);
        setFavoriteState(folder.isFavorite(), true);
        editing = false;
        dirty = false;
        unsavedLabel.setText(" ");
        contentLayout.show(contentCards, VIEW_CARD);
        updateActionState();
    }

    public void renderNavigationMessage(String title, String message) {
        selectedObject = null;
        selectedProcessId = null;
        viewerPanel.renderFolder(title, message, false);
        renderBreadcrumb(List.of(new BreadcrumbSegment("Processos", null), new BreadcrumbSegment(title, null)));
        setFavoriteState(false, false);
        editing = false;
        dirty = false;
        unsavedLabel.setText(" ");
        contentLayout.show(contentCards, VIEW_CARD);
        updateActionState();
    }

    private void setFavoriteState(boolean favorite, boolean enabled) {
        favoriteButton.setIcon(favorite ? STAR_FILLED_ICON : STAR_OUTLINE_ICON);
        favoriteButton.setToolTipText(favorite ? "Remover dos favoritos" : "Favoritar");
        favoriteButton.setEnabled(enabled);
    }

    private void renderBreadcrumb(List<BreadcrumbSegment> segments) {
        breadcrumbBar.removeAll();
        List<BreadcrumbSegment> safeSegments = segments == null || segments.isEmpty()
                ? List.of(new BreadcrumbSegment("Processos", null)) : segments;
        for (int i = 0; i < safeSegments.size(); i++) {
            BreadcrumbSegment segment = safeSegments.get(i);
            boolean isCurrent = i == safeSegments.size() - 1 || segment.folderId() == null;
            JLabel label = new JLabel(segment.label());
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 14f));
            if (isCurrent) {
                label.setForeground(Palette.TEXT_ON_DARK);
            } else {
                UUID targetFolderId = segment.folderId();
                label.setForeground(Palette.TEXT_ON_DARK_MUTED);
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent event) {
                        selectFolder(targetFolderId);
                    }

                    @Override
                    public void mouseEntered(MouseEvent event) {
                        label.setForeground(Palette.TEXT_ON_DARK);
                        label.setText("<html><u>" + segment.label() + "</u></html>");
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        label.setForeground(Palette.TEXT_ON_DARK_MUTED);
                        label.setText(segment.label());
                    }
                });
            }
            breadcrumbBar.add(label);
            if (i < safeSegments.size() - 1) {
                JLabel separator = new JLabel("  ›  ");
                separator.setForeground(Palette.TEXT_ON_DARK_MUTED);
                breadcrumbBar.add(separator);
            }
        }
        breadcrumbBar.revalidate();
        breadcrumbBar.repaint();
    }

    /** Um trecho do breadcrumb: rótulo exibido e, se navegável, o id real da pasta (senão {@code null}). */
    public record BreadcrumbSegment(String label, UUID folderId) {
    }

    public void beginEditing(ProcessDocument process) {
        editorPanel.fill(process);
        editing = true;
        dirty = false;
        unsavedLabel.setText("Modo de edição");
        contentLayout.show(contentCards, EDIT_CARD);
        updateActionState();
    }

    public ProcessEditorData readEditorData() {
        return editorPanel.readData();
    }

    public void cancelEditing() {
        editing = false;
        dirty = false;
        unsavedLabel.setText(" ");
        contentLayout.show(contentCards, VIEW_CARD);
        updateActionState();
    }

    public boolean hasUnsavedChanges() {
        return editing && dirty;
    }

    public UUID getSelectedFolderId() {
        if (selectedObject instanceof Folder folder) {
            return folder.getId();
        }
        if (selectedObject instanceof ProcessDocument process) {
            return process.getFolderId();
        }
        return selectedFolderId;
    }

    public UUID getSelectedProcessId() {
        return selectedProcessId;
    }

    public Object getSelectedObject() {
        return selectedObject;
    }

    public void selectFolder(UUID id) {
        selectNode(folderNodes.get(id));
    }

    public void selectProcess(UUID id) {
        selectNode(processNodes.get(id));
    }

    public void onSelection(Consumer<Object> listener) { selectionListener = listener; }
    public void onNewFolder(Runnable listener) { newFolderButton.addActionListener(event -> listener.run()); }
    public void onNewProcess(Runnable listener) { newProcessButton.addActionListener(event -> listener.run()); }
    public void onEdit(Runnable listener) { editButton.addActionListener(event -> listener.run()); }
    public void onRename(Runnable listener) { renameMenuItem.addActionListener(event -> listener.run()); }
    public void onMove(Runnable listener) { moveMenuItem.addActionListener(event -> listener.run()); }
    public void onDelete(Runnable listener) { deleteButton.addActionListener(event -> listener.run()); }
    public void onFavorite(Runnable listener) { favoriteListener = listener == null ? () -> { } : listener; }
    public void onRefresh(Runnable listener) { refreshMenuItem.addActionListener(event -> listener.run()); }
    public void onSave(Runnable listener) { saveButton.addActionListener(event -> listener.run()); }
    public void onCancel(Runnable listener) { cancelButton.addActionListener(event -> listener.run()); }

    public void setEditMode(boolean editMode) {
        editAuthorized = editMode;
        if (!editMode && editing) {
            cancelEditing();
        }
        updateActionState();
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(Palette.SURFACE_DARK_ALT);
        toolbar.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        toolbar.add(newFolderButton);
        toolbar.add(newProcessButton);
        toolbar.addSeparator();
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(createMoreButton());
        toolbar.add(undoButton);
        toolbar.add(redoButton);
        toolbar.add(Box.createHorizontalGlue());
        unsavedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        unsavedLabel.setForeground(Palette.WARNING);
        toolbar.add(unsavedLabel);
        toolbar.addSeparator(new Dimension(12, 0));
        toolbar.add(cancelButton);
        saveButton.putClientProperty("JButton.buttonType", "default");
        toolbar.add(saveButton);
        return toolbar;
    }

    private JButton createMoreButton() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(renameMenuItem);
        menu.add(moveMenuItem);
        menu.add(refreshMenuItem);
        moreButton.addActionListener(event -> menu.show(moreButton, 0, moreButton.getHeight()));
        return moreButton;
    }

    private JSplitPane createWorkspace() {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new NavigationRenderer());
        tree.addTreeSelectionListener(event -> handleTreeSelection());
        tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override
            public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode node) {
                    Object key = keyFor(node);
                    if (key != null) {
                        expandedKeys.add(key);
                    }
                }
            }

            @Override
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof DefaultMutableTreeNode node) {
                    expandedKeys.remove(keyFor(node));
                }
            }
        });
        tree.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int row = tree.getRowForLocation(event.getX(), event.getY());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    tree.repaint();
                }
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent event) {
                hoveredRow = -1;
                tree.repaint();
            }
        });

        JPanel navigation = new JPanel(new BorderLayout(0, 8));
        navigation.setBackground(Palette.SURFACE_DARK);
        navigation.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 8));
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());
        navigation.add(treeScroll, BorderLayout.CENTER);
        navigation.setMinimumSize(new Dimension(270, 0));

        JPanel right = new JPanel(new BorderLayout());
        JPanel breadcrumb = new JPanel(new BorderLayout());
        breadcrumb.setBackground(Palette.SURFACE_DARK_ALT);
        breadcrumb.setBorder(BorderFactory.createEmptyBorder(10, 18, 9, 18));
        breadcrumbBar.setOpaque(false);
        breadcrumb.add(breadcrumbBar, BorderLayout.CENTER);
        favoriteButton.setToolTipText("Favoritar");
        favoriteButton.setFocusable(false);
        favoriteButton.putClientProperty("JButton.buttonType", "roundRect");
        favoriteButton.putClientProperty("JComponent.arc", 8);
        favoriteButton.setFont(favoriteButton.getFont().deriveFont(java.awt.Font.PLAIN, 17f));
        favoriteButton.setMargin(new Insets(4, 12, 4, 12));
        favoriteButton.setBackground(Palette.SURFACE_DARK);
        favoriteButton.setForeground(Palette.TEXT_ON_DARK_MUTED);
        favoriteButton.setBorder(BorderFactory.createLineBorder(Palette.BORDER_DARK, 1, true));
        favoriteButton.addActionListener(event -> favoriteListener.run());
        breadcrumb.add(favoriteButton, BorderLayout.EAST);
        right.add(breadcrumb, BorderLayout.NORTH);

        contentCards.add(viewerPanel, VIEW_CARD);
        contentCards.add(editorPanel, EDIT_CARD);
        right.add(contentCards, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation, right);
        split.setDividerLocation(330);
        split.setResizeWeight(0.22);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private void installShortcuts() {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveProcess");
        getActionMap().put("saveProcess", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (saveButton.isEnabled()) {
                    saveButton.doClick();
                }
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "editProcess");
        getActionMap().put("editProcess", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (editButton.isEnabled()) {
                    editButton.doClick();
                }
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newProcess");
        getActionMap().put("newProcess", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (newProcessButton.isEnabled()) {
                    newProcessButton.doClick();
                }
            }
        });
    }

    private DefaultMutableTreeNode createFavoritesNode(List<Folder> folders, List<ProcessDocument> processes) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NavigationGroup("★ Favoritos",
                "Itens marcados como favoritos."));
        folders.forEach(folder -> node.add(new DefaultMutableTreeNode(folder)));
        processes.forEach(process -> node.add(new DefaultMutableTreeNode(process)));
        return node;
    }

    private DefaultMutableTreeNode createRecentNode(List<ProcessDocument> processes) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new NavigationGroup("◷ Recentes",
                "Últimos processos abertos."));
        processes.forEach(process -> node.add(new DefaultMutableTreeNode(process)));
        return node;
    }

    private void handleTreeSelection() {
        Object node = tree.getLastSelectedPathComponent();
        if (!(node instanceof DefaultMutableTreeNode treeNode)) {
            return;
        }
        Object object = treeNode.getUserObject();
        if (object instanceof NavigationGroup group) {
            renderNavigationMessage(group.title(), group.description());
            return;
        }
        selectedObject = object;
        if (object instanceof Folder folder) {
            selectedFolderId = folder.getId();
            selectedProcessId = null;
        } else if (object instanceof ProcessDocument process) {
            selectedFolderId = process.getFolderId();
            selectedProcessId = process.getId();
        }
        selectionListener.accept(object);
        updateActionState();
    }

    private void markDirty() {
        if (!editing) {
            return;
        }
        dirty = true;
        unsavedLabel.setText("● Alterações não salvas");
        updateActionState();
    }

    private void updateActionState() {
        boolean hasFolder = getSelectedFolderId() != null;
        boolean hasSelection = selectedObject instanceof Folder || selectedObject instanceof ProcessDocument;
        boolean hasProcess = selectedObject instanceof ProcessDocument || selectedProcessId != null;
        newFolderButton.setEnabled(editAuthorized);
        newProcessButton.setEnabled(editAuthorized && hasFolder);
        editButton.setEnabled(editAuthorized && hasProcess && !editing);
        renameMenuItem.setEnabled(editAuthorized && hasSelection && !editing);
        moveMenuItem.setEnabled(editAuthorized && hasSelection && !editing);
        deleteButton.setEnabled(editAuthorized && hasSelection && !editing);
        refreshMenuItem.setEnabled(!editing || !dirty);
        viewerPanel.setEditAvailable(editButton.isEnabled());
        undoButton.setVisible(editing);
        redoButton.setVisible(editing);
        undoButton.setEnabled(editing && editorPanel.canUndo());
        redoButton.setEnabled(editing && editorPanel.canRedo());
        tree.setEnabled(!editing);
        saveButton.setVisible(editing);
        cancelButton.setVisible(editing);
        saveButton.setContentAreaFilled(editing);
        saveButton.setBackground(Palette.ACCENT);
        saveButton.setEnabled(editAuthorized && editing && dirty);
        cancelButton.setEnabled(editing);
    }

    private void restoreExpandedState() {
        for (int row = 0; row < tree.getRowCount(); row++) {
            TreePath path = tree.getPathForRow(row);
            Object last = path.getLastPathComponent();
            if (last instanceof DefaultMutableTreeNode node && expandedKeys.contains(keyFor(node))) {
                tree.expandRow(row);
            }
        }
    }

    private Object keyFor(DefaultMutableTreeNode node) {
        Object object = node.getUserObject();
        if (object instanceof Folder folder) {
            return folder.getId();
        }
        if (object instanceof NavigationGroup group) {
            return group.title();
        }
        return null;
    }

    private void restoreSelection() {
        if (selectedProcessId != null && processNodes.containsKey(selectedProcessId)) {
            selectNode(processNodes.get(selectedProcessId));
        } else if (selectedFolderId != null && folderNodes.containsKey(selectedFolderId)) {
            selectNode(folderNodes.get(selectedFolderId));
        }
    }

    private void selectNode(DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.setForeground(Palette.TEXT_ON_DARK);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        return button;
    }

    private record NavigationGroup(String title, String description) {
        @Override
        public String toString() {
            return title;
        }
    }

    private final class NavigationRenderer extends DefaultTreeCellRenderer {
        private static final com.formdev.flatlaf.extras.FlatSVGIcon FOLDER_ICON =
                new com.formdev.flatlaf.extras.FlatSVGIcon("icons/svg/folder.svg");
        private static final com.formdev.flatlaf.extras.FlatSVGIcon DOCUMENT_ICON =
                new com.formdev.flatlaf.extras.FlatSVGIcon("icons/svg/document.svg");

        private NavigationRenderer() {
            setBackgroundSelectionColor(Palette.ACCENT);
            setTextSelectionColor(Color.WHITE);
            setBackgroundNonSelectionColor(Palette.SURFACE_DARK);
            setTextNonSelectionColor(Palette.TEXT_ON_DARK);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setOpaque(!selected && row == hoveredRow);
            if (!selected && row == hoveredRow) {
                setBackground(Palette.SURFACE_DARK_ALT);
            }
            if (value instanceof DefaultMutableTreeNode node) {
                Object object = node.getUserObject();
                if (object instanceof Folder folder) {
                    setText(folder.getName());
                    setIcon(FOLDER_ICON);
                } else if (object instanceof ProcessDocument process) {
                    setText(process.getTitle());
                    setIcon(DOCUMENT_ICON);
                } else if (object instanceof NavigationGroup group) {
                    setText(group.title());
                    setFont(getFont().deriveFont(Font.BOLD));
                    setIcon(null);
                }
            }
            return this;
        }
    }
}
