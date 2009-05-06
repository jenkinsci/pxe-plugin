package hudson.plugins.pxe;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

/**
 * CentOS boot configuration.
 *
 * <p>
 * CentOS seems to use a slightly different version of kickstart.
 *
 * @author Kohsuke Kawaguchi
 */
public class CentOSBootConfiguration extends RedHatBootConfiguration {
    @DataBoundConstructor
    public CentOSBootConfiguration(File iso, String password, String additionalPackages) {
        super(iso, password, additionalPackages);
    }

    @Extension
    public static class DescriptorImpl extends RedHatBootConfiguration.DescriptorImpl {
        public String getDisplayName() {
            return "CentOS";
        }
    }
}
