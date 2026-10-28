package br.com.magnobraccini.helplixo.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.util.List;

/**
 * Coloca conteúdo na área de transferência do sistema operacional imitando o "Copiar" do
 * Explorer: um arquivo copiado por aqui pode ser colado (Ctrl+V) em qualquer pasta, e-mail ou
 * sessão de área de trabalho remota que aceite arquivos colados — não é só o caminho como texto.
 */
public final class ClipboardFiles {
    private ClipboardFiles() {
    }

    /** Copia o arquivo em si (como se fosse Ctrl+C no Explorer) para a área de transferência. */
    public static void copyFile(File file) {
        Transferable transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.javaFileListFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return List.of(file);
            }
        };
        clipboard().setContents(transferable, null);
    }

    /** Copia um texto simples (usado como alternativa quando o item não é um arquivo local). */
    public static void copyText(String text) {
        clipboard().setContents(new StringSelection(text), null);
    }

    private static Clipboard clipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }
}
