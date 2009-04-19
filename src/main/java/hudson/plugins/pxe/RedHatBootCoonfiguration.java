package hudson.plugins.pxe;

import hudson.Extension;
import static hudson.util.FormValidation.error;
import org.jvnet.hudson.tftpd.Data;
import org.kohsuke.loopy.FileEntry;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RedHat/Fedora boot configuration.
 *
 * @author Kohsuke Kawaguchi
 */
public class RedHatBootCoonfiguration extends LinuxBootConfiguration {
    @DataBoundConstructor
    public RedHatBootCoonfiguration(File iso) {
        super(iso);
    }

    protected String getIdSeed() {
        // TODO: get distro name and release
        return "redhat";
    }

    protected FileEntry getTftpIsoMountDir(ISO9660FileSystem fs) throws IOException {
        return fs.get("/images/pxeboot");
    }

    /**
     * Serves menu.txt by replacing variables.
     */
    public Data tftp(String fileName) throws IOException {
        if(fileName.equals("splash.jpg")) {
            ISO9660FileSystem fs = new ISO9660FileSystem(iso,false);
            return new FileEntryData(fs.grab("/isolinux/splash.jpg"));
        }

        return super.tftp(fileName);
    }

    /**
     * Serves the kickstart file
     */
    public void doKickstart(StaplerResponse rsp) throws IOException {
        serveMacroExpandedResource(rsp,"kickstart.txt");
    }

    @Extension
    public static class DescriptorImpl extends IsoBasedBootConfigurationDescriptor {
        public String getDisplayName() {
            return "RedHat/Fedora";
        }

        /**
         * This returns string like "Fedora 10"
         *
         * TODO: where can we get the architecture information?
         */
        protected String getReleaseInfo(File iso) throws IOException {
            ISO9660FileSystem fs=null;
            try {
                try {
                    fs = new ISO9660FileSystem(iso,false);
                } catch (IOException e) {
                    LOGGER.log(Level.INFO,iso+" isn't an ISO file?",e);
                    throw error(iso+" doesn't look like an ISO file");
                }

                FileEntry info = fs.get("/media.repo");
                if(info==null)
                    throw error(iso+" doesn't look like a RedHat/Fedora CD/DVD image");

                /* On Fedora 10 DVD, this file looks like:
                        [InstallMedia]
                        name=Fedora 10
                        mediaid=1227142402.812888
                        metadata_expire=-1
                        gpgcheck=0
                        cost=500
                 */
                BufferedReader r = new BufferedReader(new InputStreamReader(info.read()));
                try {
                    String line;
                    while((line=r.readLine())!=null) {
                        if(line.startsWith("name="))
                            return line.substring(5);
                    }
                    throw error(iso+" doesn't contain the name entry in media.repo");
                } finally {
                    r.close();
                }
            } finally {
                if(fs!=null)
                    fs.close();
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(RedHatBootCoonfiguration.class.getName());
}
