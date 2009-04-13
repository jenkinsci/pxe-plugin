package hudson.plugins.pxe;

import hudson.model.Describable;
import hudson.model.Hudson;
import org.jvnet.hudson.tftpd.Data;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class BootConfiguration implements Describable<BootConfiguration> {
    public BootImage getDescriptor() {
        return (BootImage) Hudson.getInstance().getDescriptor(getClass());
    }

    public abstract Data tftp(String fileName);
}
