package hudson.plugins.pxe;

import hudson.Plugin;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.os.SU;
import hudson.util.DescribableList;
import hudson.util.Secret;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    private String rootUserName;
    private Secret rootPassword;
    private final DescribableList<BootConfiguration,BootImage> bootConfigurations = new DescribableList<BootConfiguration, BootImage>(this);

    @Override
    public void start() throws Exception {
        load();

        TaskListener listener = new StreamTaskListener(getLogFile());
        SU.executeAsync(listener,rootUserName,rootPassword==null?null:rootPassword.toString(),new PXEBootProcess(new PathResolverImpl()));
    }

    public File getLogFile() {
        return new File(Hudson.getInstance().getRootDir(),"pxe.log");
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public Secret getRootPassword() {
        return rootPassword;
    }

    public DescribableList<BootConfiguration,BootImage> getBootConfigurations() {
        return bootConfigurations;
    }

    public void setRootAccount(String userName, Secret password) throws IOException {
        this.rootUserName = Util.fixEmptyAndTrim(userName);
        this.rootPassword = password;
        save();
    }
}
