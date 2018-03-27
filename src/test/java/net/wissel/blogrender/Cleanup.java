package net.wissel.blogrender;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class Cleanup {

    public static void main(String[] args) {
        Cleanup cleanup = new Cleanup();
        cleanup.go();
        System.out.println("Done");
    }

    private final Collection<String> deleteEntries = new ArrayList<>();
    
    private void go() {
        final Config config = Config.get(Config.CONFIG_NAME);
        this.cleanDirectory(config.sourceDirectory + "/" + config.documentDirectory);
        
    }

    private void cleanDirectory(String documentDirectory) {
        File curDir = new File(documentDirectory);
        if (!curDir.exists()) {
            System.err.println("Directory doesn't exist:"+documentDirectory);
            return;
        }
        
        if (curDir.isDirectory()) {
            for (final String curFile : curDir.list()) {
                this.cleanDirectory(curDir.getPath() + "/" + curFile);
            }
        } else {
            this.deleteEntries.forEach(e -> {
                if (curDir.getName().endsWith(e)) {
                    System.out.print(curDir.getName());
                    curDir.delete();
                    System.out.println(" - deleted");
                }
            });
        }
        
    }

    
    public Cleanup() {
        deleteEntries.add(".html");
        deleteEntries.add(".json");
        deleteEntries.add(".more");
    }
    
}
