package hudson.plugins.pxe;

import hudson.Plugin;
import hudson.Util;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.os.SU;
import hudson.remoting.VirtualChannel;
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
    private VirtualChannel channel;
    private final DescribableList<BootConfiguration, BootConfigurationDescriptor> bootConfigurations = new DescribableList<BootConfiguration, BootConfigurationDescriptor>(this);

    /**
     * Initialization needs to happen after all the boot image plugins are loaded, so {@link #start()} won't do.
     */
    @Override
    public void postInitialize() throws Exception {
        load();

        TaskListener listener = new StreamTaskListener(getLogFile());
        channel = SU.start(listener, rootUserName, rootPassword == null ? null : rootPassword.toString());
        channel.callAsync(new PXEBootProcess(new PathResolverImpl()));
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

    public DescribableList<BootConfiguration, BootConfigurationDescriptor> getBootConfigurations() {
        return bootConfigurations;
    }

    public void setRootAccount(String userName, Secret password) throws IOException {
        this.rootUserName = Util.fixEmptyAndTrim(userName);
        this.rootPassword = password;
        save();
    }

    public VirtualChannel getChannel() {
        return channel;
    }
}
