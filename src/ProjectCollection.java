import java.util.HashMap;

/**
 * Created by nathanaelsmith on 8/10/16.
 */
public class ProjectCollection {

    protected static HashMap<String, TimeTrackingProject> hash = new <String, TimeTrackingProject>HashMap<String, TimeTrackingProject>();

    public TimeTrackingProject getProject(String id) {
        TimeTrackingProject project = hash.get(id);
        if (project == null) {
            project = new TimeTrackingProject();
            project.id = id;
            hash.put(id , project);
            return project;
        }
        return hash.get(id);
    }

}
