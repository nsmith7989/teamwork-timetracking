import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.math.BigDecimal;

/**
 * Created by nathanaelsmith on 8/10/16.
 */
public class CustomMouseListener implements EditorMouseListener {

    @Override
    public void mousePressed(EditorMouseEvent editorMouseEvent) {
        final FileDocumentManager instance = FileDocumentManager.getInstance();
        final VirtualFile file = instance.getFile(editorMouseEvent.getEditor().getDocument());
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

    @Override
    public void mouseClicked(EditorMouseEvent editorMouseEvent) {
    }

    @Override
    public void mouseReleased(EditorMouseEvent editorMouseEvent) {
    }

    @Override
    public void mouseEntered(EditorMouseEvent editorMouseEvent) {
    }

    @Override
    public void mouseExited(EditorMouseEvent editorMouseEvent) {
    }

}
