package hudson.plugins.pxe;

import hudson.model.Describable;
import hudson.model.Hudson;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class BootConfiguration implements Describable<BootConfiguration> {
    public BootImage getDescriptor() {
        return (BootImage) Hudson.getInstance().getDescriptor(getClass());
    }
}
