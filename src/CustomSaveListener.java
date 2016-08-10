import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class CustomSaveListener extends FileDocumentManagerAdapter {

    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        String currentFile = FileDocumentManager.getInstance().getFile(document).getPath();
        if (Teamwork.shouldLogFile(currentFile)) {
            BigDecimal currentTime = Teamwork.getCurrentTimestamp();
            Teamwork.appendHeartbeat(currentTime, currentFile, true);
        }
    }
}
