package hudson.plugins.pxe;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Descriptor;
import hudson.model.Hudson;

/**
 * Extension point for bootable images.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BootImage extends Descriptor<BootConfiguration> implements ExtensionPoint {
    protected BootImage(Class<? extends BootConfiguration> clazz) {
        super(clazz);
    }

    protected BootImage() {
    }

    /**
     * Returns all the registered {@link BootImage}s.
     */
    public static DescriptorExtensionList<BootConfiguration,BootImage> all() {
        return Hudson.getInstance().getDescriptorList(BootConfiguration.class);
    }
}
