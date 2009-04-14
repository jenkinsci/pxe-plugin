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
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class PluginImpl extends Plugin {
    private String rootUserName;
    private Secret rootPassword;
    private transient VirtualChannel channel;
    private final DescribableList<BootConfiguration, BootConfigurationDescriptor> bootConfigurations = new DescribableList<BootConfiguration, BootConfigurationDescriptor>(this);
    private String tftpAddress;

    /**
     * Initialization needs to happen after all the boot image plugins are loaded, so {@link #start()} won't do.
     */
    @Override
    public void postInitialize() throws Exception {
        load();
        restartPXE();
    }

    public synchronized void restartPXE() throws IOException, InterruptedException {
        if(tftpAddress==null) {
            LOGGER.warning("Not starting TFTP/ProxyDHCP service due to incomplete configuration");
            return;
        }
        
        if(channel!=null) {
            LOGGER.info("Stopping TFTP/ProxyDHCP service");
            channel.close();
            channel.join(3000);
        }
        LOGGER.info("Starting TFTP/ProxyDHCP service");
        TaskListener listener = new StreamTaskListener(getLogFile());
        channel = SU.start(listener, rootUserName, rootPassword == null ? null : rootPassword.toString());
        channel.callAsync(new PXEBootProcess(new PathResolverImpl(), tftpAddress));
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

    public void setTftpAddress(String address) throws IOException {
        this.tftpAddress = address;
        save();
    }

    public String getTftpAddress() {
        return tftpAddress;
    }

    public VirtualChannel getChannel() {
        return channel;
    }

    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

}
