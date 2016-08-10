import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.math.BigDecimal;


public class GetSessionTime extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        String projectName = Teamwork.getProjectName();
        BigDecimal time = Teamwork.getTotalTime(Teamwork.getProjectName());

        Notifications.Bus.notify(
                new Notification("timekeeper", projectName + " - Time Tracking", time + " minutes of total time", NotificationType.INFORMATION)
        );
    }
}
