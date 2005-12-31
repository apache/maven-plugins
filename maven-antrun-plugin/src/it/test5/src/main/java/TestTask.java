import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class TestTask extends Task {

    public void execute() throws BuildException {
        Project p = this.getProject();
        System.out.println("sourceDirectory:" + p.getProperty("project.build.sourceDirectory"));
        System.out.println("project.cmdline:" + p.getProperty("project.cmdline"));
        System.out.println("basedir:" + p.getProperty("basedir"));
    }

}
