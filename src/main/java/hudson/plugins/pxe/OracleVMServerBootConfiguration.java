package hudson.plugins.pxe;

import hudson.Extension;
import org.kohsuke.loopy.FileEntry;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;

/**
 * OracleVM Server.
 *
 * Based on 2.2.0.
 *
 * <h2>Notes</h2>
 * <p>
 * Fails to boot if the memory is less than 512M.
 *
 * @author Kohsuke Kawaguchi
 */
public class OracleVMServerBootConfiguration extends RedHatBootConfiguration {
    @DataBoundConstructor
    public OracleVMServerBootConfiguration(File iso, String password, String additionalPackages) {
        super(iso, password, additionalPackages);
    }

    protected FileEntry getTftpIsoMountDir(ISO9660FileSystem fs) throws IOException {
        return fs.getRootEntry();
    }

    public void doMinimumKickstart(StaplerResponse rsp) throws IOException {
        serveMacroExpandedResource(rsp,"minimum-kickstart.txt");
    }

    @Extension
    public static class DescriptorImpl extends RedHatBootConfiguration.DescriptorImpl {
        public String getDisplayName() {
            return "Oracle VM Server";
        }
    }
}
