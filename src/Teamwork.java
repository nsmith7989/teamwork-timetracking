import com.intellij.AppTopics;
import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.util.PlatformUtils;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.math.RoundingMode.*;

public class Teamwork implements ApplicationComponent {

    public static String IDE_NAME;
    public static String IDE_VERSION;
    public static BigDecimal lastTime = new BigDecimal(0);
    public static BigDecimal timeout = new BigDecimal(60); // in seconds
    public static String lastFile = null;
    public static MessageBusConnection connection;
    private final int queueTimeoutSeconds = 10;
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static ConcurrentLinkedQueue<Heartbeat> heartbeatsQueue = new ConcurrentLinkedQueue<Heartbeat>();
    private static ScheduledFuture<?> scheduledFixture;
    public static final Logger log = Logger.getInstance("Teamwork");
    public static BigDecimal totalTime = new BigDecimal(0);
    public static final BigDecimal FREQUENCY = new BigDecimal(2 * 60); // max secs between heartbeats for continuous coding

    public static final ProjectCollection projectCollection = new ProjectCollection();


    public Teamwork() {
    }

    @Override
    public void initComponent() {
        // run on startup
        IDE_NAME = PlatformUtils.getPlatformPrefix();
        IDE_VERSION = ApplicationInfo.getInstance().getFullVersion();

        setupEventListeners();
        setupQueueProcessor();
        log.setLevel(Level.DEBUG);
    }

    public static boolean shouldLogFile(String file) {
        if (file.equals("atlassian-ide-plugin.xml") || file.contains("/.idea/workspace.xml")) {
            return false;
        }
        return true;
    }


    private void setupEventListeners() {

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                // save file
                MessageBus bus = ApplicationManager.getApplication().getMessageBus();
                connection = bus.connect();
                connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new CustomSaveListener());

                // edit document
                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new CustomDocumentListener());

                // mouse press
                EditorFactory.getInstance().getEventMulticaster().addEditorMouseListener(new CustomMouseListener());

                // scroll document
                EditorFactory.getInstance().getEventMulticaster().addVisibleAreaListener(new CustomVisibleAreaListener());


            }
        });

    }

    @Override
    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Teamwork";
    }

    public static BigDecimal getCurrentTimestamp() {
        return new BigDecimal(String.valueOf(System.currentTimeMillis() / 1000.0)).setScale(4, BigDecimal.ROUND_HALF_UP);
    }

    public static String getProjectName() {
        DataContext dataContext = DataManager.getInstance().getDataContext();
        if (dataContext != null) {
            Project project = null;
            try {
                project = PlatformDataKeys.PROJECT.getData(dataContext);
            } catch (NoClassDefFoundError e) {
                try {
                    project = DataKeys.PROJECT.getData(dataContext);
                } catch (NoClassDefFoundError ex) {}
            }
            if (project != null) {
                return project.getName();
            }
        }
        return null;
    }

    public static void checkForTimeout(BigDecimal time, TimeTrackingProject project)
    {
        BigDecimal timeSinceLast = time.subtract(Teamwork.lastTime);
        if (timeout.compareTo(timeSinceLast) == -1) {
            Teamwork.lastTime = new BigDecimal(0);
        }
    }

    public static void appendHeartbeat(BigDecimal time, String file, boolean isWrite) {

        final String project = Teamwork.getProjectName();


        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final TimeTrackingProject projectItem = projectCollection.getProject(project);
            checkForTimeout(time, projectItem);
            Heartbeat h = new Heartbeat();
            h.entity = file;
            h.timestamp = time;
            h.isWrite = isWrite;
            h.project = project;
            if (projectItem.lastUpdateTime.equals(new BigDecimal(0))) {
                h.timeSinceLast = new BigDecimal(0);
            } else {
                h.timeSinceLast = time.subtract(projectItem.lastUpdateTime);
            }

            Teamwork.lastFile = file;
            projectItem.lastUpdateTime = time;
            heartbeatsQueue.add(h);
        });
    }


    private void setupQueueProcessor() {
        final Runnable handler = new Runnable() {
            public void run() {
                processHeartbeatQueue();
            }
        };
        long delay = queueTimeoutSeconds;
        scheduledFixture = scheduler.scheduleAtFixedRate(handler, delay, delay, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void processHeartbeatQueue()
    {
        Heartbeat heartbeat = heartbeatsQueue.poll();

        if (heartbeat == null)
            return;

        // get all extra heartbeats from queue
        ArrayList<Heartbeat> extraHeartbeats = new ArrayList<Heartbeat>();
        while (true) {
            Heartbeat h = heartbeatsQueue.poll();
            if (h == null)
                break;
            extraHeartbeats.add(h);
        }

        sendHeartBeat(heartbeat, extraHeartbeats);

    }

    private static BigDecimal secondsToMinutes(BigDecimal seconds) {
        return seconds.divide(new BigDecimal(60), 1, CEILING).setScale(1, BigDecimal.ROUND_HALF_EVEN);
    }

    public static BigDecimal getTotalTime(String projectId)
    {
        TimeTrackingProject project = projectCollection.getProject(projectId);
        if (project != null) {
            return secondsToMinutes(project.totalTime);
        } else {
            return new BigDecimal(0);
        }

    }

    private void sendHeartBeat(Heartbeat heartbeat, ArrayList<Heartbeat> extraHeartbeats) {

        if (heartbeat.project != null) {
            final TimeTrackingProject project = projectCollection.getProject(heartbeat.project);
            // add the current heartbeat to time
            project.totalTime = project.totalTime.add(heartbeat.timeSinceLast);
            System.out.print("Time" + project.totalTime);
        }


        for(Heartbeat item : extraHeartbeats) {
            if (item.project == null) {
                continue;
            }
            TimeTrackingProject itemProject = projectCollection.getProject(item.project);
            itemProject.totalTime = itemProject.totalTime.add(item.timeSinceLast);
        }


    }

    public static boolean enoughTimePassed(BigDecimal currentTime) {
        return Teamwork.lastTime.add(FREQUENCY).compareTo(currentTime) < 0;
    }
}
