package hudson.plugins.pxe;

import hudson.Extension;
import org.jvnet.hudson.tftpd.Data;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hudson.util.FormValidation.error;

/**
 * Boots VMWare ESXi.
 *
 * @author Kohsuke Kawaguchi
 */
public class VMWareESXiBootConfiguration extends IsoBasedBootConfiguration {
    @DataBoundConstructor
    public VMWareESXiBootConfiguration(File iso) {
        super(iso);
    }

    @Override
    protected String getIdSeed() {
        return "esxi";
    }

    @Override
    public Data tftp(String fileName) throws IOException {
        // TODO: closing this file system voids FileEntryData. Fix it
        ISO9660FileSystem fs = new ISO9660FileSystem(iso, false);
        return new FileEntryData(fs.getRootEntry().grab(fileName));
    }

    @Override
    public String getPxeLinuxConfigFragment() throws IOException {
        return String.format("LABEL %1$s\n" +
                "    MENU LABEL %2$s\n" +
                "    KERNEL mboot.c32\n" +
                // the following list is taken from 4.0. Maybe we should parse isolinux.cfg? 
                "    APPEND %3$s/vmkboot.gz --- %3$s/vmkernel.gz --- %3$s/sys.vgz --- %3$s/cim.vgz --- %3$s/ienviron.tgz --- %3$s/image.tgz --- %3$s/install.tgz\n",
                getId(), getDisplayName(), getId());
    }

    @Extension
    public static class DescriptorImpl extends IsoBasedBootConfigurationDescriptor {
        public String getDisplayName() {
            return "VMWare ESXi";
        }

        protected String getReleaseInfo(File iso) throws IOException {
            ISO9660FileSystem fs=null;
            try {
                try {
                    fs = new ISO9660FileSystem(iso,false);
                } catch (IOException e) {
                    LOGGER.log(Level.INFO,iso+" isn't an ISO file?",e);
                    throw error(iso+" doesn't look like an ISO file");
                }

                if(fs.get("/cim.vgz")==null || fs.get("/vmkboot.gz")==null)
                    throw error(iso+" doesn't look like an ESXi CD image");

                return "VMWare ESXi";
            } finally {
                if(fs!=null)
                    fs.close();
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(VMWareESXiBootConfiguration.class.getName());
}
