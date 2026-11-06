package br.com.magnobraccini.helplixo.view.process;

import javax.swing.event.UndoableEditEvent;
import javax.swing.text.Document;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Histórico de desfazer/refazer de um único campo de texto. Agrupa a digitação contínua
 * (pausas menores que {@link #TYPING_PAUSE_MS}) em um só passo — Ctrl+Z desfaz uma palavra ou
 * frase, não uma letra — e permite agrupar operações compostas (ex.: converter parágrafos em
 * lista) em uma única entrada.
 */
final class TextUndo {
    private static final long TYPING_PAUSE_MS = 900;

    private final UndoManager manager = new UndoManager();
    private CompoundEdit typing;
    private CompoundEdit explicit;
    private long lastEditAt;
    private int suspended;
    private Runnable listener = () -> { };

    TextUndo(Document document) {
        manager.setLimit(500);
        document.addUndoableEditListener(this::record);
    }

    void onStateChange(Runnable listener) { this.listener = listener == null ? () -> { } : listener; }

    private void record(UndoableEditEvent event) {
        if (suspended > 0) return;
        UndoableEdit edit = event.getEdit();
        if (explicit != null) {
            explicit.addEdit(edit);
            return;
        }
        long now = System.currentTimeMillis();
        boolean isTyping = edit instanceof javax.swing.event.DocumentEvent documentEvent
                && documentEvent.getType() != javax.swing.event.DocumentEvent.EventType.CHANGE;
        if (isTyping && typing != null && now - lastEditAt < TYPING_PAUSE_MS) {
            typing.addEdit(edit);
        } else {
            closeTyping();
            if (isTyping) {
                typing = new CompoundEdit();
                typing.addEdit(edit);
                manager.addEdit(typing);
            } else {
                manager.addEdit(edit);
            }
        }
        lastEditAt = now;
        listener.run();
    }

    private void closeTyping() {
        if (typing != null) {
            typing.end();
            typing = null;
        }
    }

    /** Executa {@code operation} registrando todas as alterações como um único passo de desfazer. */
    void compound(Runnable operation) {
        closeTyping();
        CompoundEdit edit = new CompoundEdit();
        explicit = edit;
        try {
            operation.run();
        } finally {
            explicit = null;
            edit.end();
            if (edit.canUndo()) manager.addEdit(edit);
            listener.run();
        }
    }

    /** Executa {@code operation} sem registrar histórico (carregamento de conteúdo). */
    void silently(Runnable operation) {
        suspended++;
        try { operation.run(); } finally { suspended--; }
    }

    boolean canUndo() { return typing != null || manager.canUndo(); }
    boolean canRedo() { return typing == null && manager.canRedo(); }

    void undo() {
        closeTyping();
        try { if (manager.canUndo()) manager.undo(); } catch (CannotUndoException ignored) { }
        listener.run();
    }

    void redo() {
        closeTyping();
        try { if (manager.canRedo()) manager.redo(); } catch (CannotRedoException ignored) { }
        listener.run();
    }

    void discardAll() {
        typing = null;
        manager.discardAllEdits();
        listener.run();
    }
}
