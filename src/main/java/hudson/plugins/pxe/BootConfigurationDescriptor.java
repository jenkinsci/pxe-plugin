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
public abstract class BootConfigurationDescriptor extends Descriptor<BootConfiguration> implements ExtensionPoint {
    protected BootConfigurationDescriptor(Class<? extends BootConfiguration> clazz) {
        super(clazz);
    }

    protected BootConfigurationDescriptor() {
    }
}
