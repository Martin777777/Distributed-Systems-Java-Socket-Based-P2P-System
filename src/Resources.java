import java.util.ArrayList;

/**
 *
 * This class is to record all the resources that exists in the system as we assume that peers knows all the resources in the system
 *
 * @author Ma Zixao
 *
 */

public class Resources {
    private static ArrayList<String> resources = new ArrayList<>();
    public static void add(String id) {
        if (!resources.contains(id)) {
            resources.add(id);
        }
    }

    public static void remove(String id) {
        resources.remove(id);
    }

    public static ArrayList<String> getAllResources() {
        return resources;
    }
}
