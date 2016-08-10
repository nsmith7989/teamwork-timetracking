import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.math.BigDecimal;

/**
 * Created by nathanaelsmith on 8/10/16.
 */
public class CustomVisibleAreaListener implements VisibleAreaListener {

    @Override
    public void visibleAreaChanged(VisibleAreaEvent visibleAreaEvent) {
        final FileDocumentManager instance = FileDocumentManager.getInstance();
        final VirtualFile file = instance.getFile(visibleAreaEvent.getEditor().getDocument());
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
