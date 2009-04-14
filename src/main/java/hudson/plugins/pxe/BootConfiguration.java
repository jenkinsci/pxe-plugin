package hudson.plugins.pxe;

import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.DescriptorExtensionList;
import org.jvnet.hudson.tftpd.Data;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class BootConfiguration implements Describable<BootConfiguration> {
    /**
     * For serving dynamic data from TFTP, it's often useful to have an unique ID per {@link BootConfiguration}.
     * This method provides that.
     */
    public String getId() {
        return String.valueOf(hashCode());
    }

    public BootConfigurationDescriptor getDescriptor() {
        return (BootConfigurationDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

    /**
     * Serves data from TFTP.
     *
     * <p>
     * This mechanism is useful when you need to generate the data to be served on the fly.
     * Static resources can be more easily served by simply placing them as resources
     * under /tftp.
     *
     * <p>
     * The TFTP file namespace is a shared resources among all {@link BootConfiguration}s,
     * so plugins are expected to use some prefix to avoid collisions.
     *
     * @return
     *      null if no such file exists, as far as this plugin is concerned.
     *      The PXE plugin will continue to search other {@link BootConfiguration}s to
     *      see if anyone understands it.
     * @throws IOException
     *      If a problem occurs. The PXE plugin will abort the search and the download will fail.
     */
    public Data tftp(String fileName) throws IOException {
        return null;
    }

    /**
     * Returns the fragment to be merged into <tt>pxelinux.cfg/default</tt>.
     */
    public abstract String getPxeLinuxConfigFragment() throws IOException;

    /**
     * Returns all the registered {@link BootConfigurationDescriptor}s.
     */
    public static DescriptorExtensionList<BootConfiguration, BootConfigurationDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(BootConfiguration.class);
    }
}
