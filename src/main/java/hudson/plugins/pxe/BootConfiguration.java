package hudson.plugins.pxe;

import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.DescriptorExtensionList;
import org.jvnet.hudson.tftpd.Data;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class BootConfiguration implements Describable<BootConfiguration> {
    public BootConfigurationDescriptor getDescriptor() {
        return (BootConfigurationDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

    public abstract Data tftp(String fileName);

    /**
     * Returns all the registered {@link BootConfigurationDescriptor}s.
     */
    public static DescriptorExtensionList<BootConfiguration, BootConfigurationDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(BootConfiguration.class);
    }
}
