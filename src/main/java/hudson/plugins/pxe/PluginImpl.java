package hudson.plugins.pxe;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.os.SU;
import hudson.util.Secret;
import hudson.util.StreamTaskListener;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    /*package*/ String rootUserName;
    /*package*/ Secret rootPassword;

    @Override
    public void start() throws Exception {
        load();

        TaskListener listener = new StreamTaskListener(getLogFile());
        SU.execute(listener,rootUserName,rootPassword==null?null:rootPassword.toString(),new PXEBootProcess());
    }

    public File getLogFile() {
        return new File(Hudson.getInstance().getRootDir(),"pxe.log");
    }
}
