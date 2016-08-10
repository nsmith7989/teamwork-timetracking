import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.math.BigDecimal;


public class CustomDocumentListener implements DocumentListener {

    @Override
    public void beforeDocumentChange(DocumentEvent documentEvent) {
    }

    @Override
    public void documentChanged(DocumentEvent documentEvent) {
        final FileDocumentManager instance = FileDocumentManager.getInstance();
        final VirtualFile file = instance.getFile(documentEvent.getDocument());

        if (file != null && !file.getUrl().startsWith("mock://")) {
            final String currentFile = file.getPath();
            if (Teamwork.shouldLogFile(currentFile)) {
                BigDecimal currentTime = Teamwork.getCurrentTimestamp();
                if (!currentFile.equals(Teamwork.lastFile) || Teamwork.enoughTimePassed(currentTime)) {
                    Teamwork.appendHeartbeat(currentTime, currentFile, false);
                }
            }
        }

    }
}
