package hudson.plugins.pxe;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.plugins.pxe.IsoBasedBootConfiguration;
import hudson.plugins.pxe.PXE;
import static hudson.util.FormValidation.error;
import org.apache.commons.io.IOUtils;
import org.kohsuke.loopy.FileEntry;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.DataBoundConstructor;
import org.jvnet.hudson.tftpd.Data;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.InetAddress;

/**
 * OpenSolaris boot.
 *
 * @author Kohsuke Kawaguchi
 */
public class OpenSolarisBootConfiguration extends IsoBasedBootConfiguration {
    @DataBoundConstructor
    public OpenSolarisBootConfiguration(File iso) {
        super(iso);
    }

    protected String getIdSeed() {
        // try to extract a short name from the release information
        Pattern p = Pattern.compile("snv_[^ ]+");
        Matcher m = p.matcher(getRelease());
        if(m.find())    return m.group(0);
        return "opensolaris";
    }

    /**
     * Chainboot to pxegrub, which in turn knows how to boot Solaris.
     *
     * Solaris boot requires $ISADIR variable that we don't know how to set.
     */
    public String getPxeLinuxConfigFragment() throws IOException {
//        return String.format("LABEL %1$s\n" +
//                "    MENU LABEL %2$s\n" +
//                "    KERNEL pxechain.com\n" +
//                "    APPEND ::%3$s/boot/grub/pxegrub \n",
//                getId(), getDisplayName(), getId() );
        String baseUrl = Hudson.getInstance().getRootUrl();
        String host = new URL(baseUrl).getHost();
        baseUrl = baseUrl.replace(host,InetAddress.getByName(host).getHostAddress());

        String httpIsoImage = String.format("%1$s/pxe/configuration/%2$s/image",
                baseUrl,getId());
        return String.format("LABEL %1$s\n" +
                "    MENU LABEL %2$s\n" +
                "    KERNEL mboot.c32\n" +
                "    APPEND -solaris %1$s/boot/platform/i86pc/kernel/unix -v -m verbose -B install_media=%3$s,install_boot=%3$s/boot,livemode=text --- %1$s/boot/x86.microroot\n",
                getId(), getDisplayName(), httpIsoImage );
    }

    @Override
    public Data tftp(String fileName) throws IOException {
        // TODO: closing this file system voids FileEntryData. Fix it
        ISO9660FileSystem fs = new ISO9660FileSystem(iso, false);
        return new FileEntryData(fs.getRootEntry().grab(fileName));
    }

    @Extension
    public static class DescriptorImpl extends IsoBasedBootConfigurationDescriptor {
        public String getDisplayName() {
            return "OpenSolaris";
        }

        /**
         * This is like "OpenSolaris 2008.11 svnc_101b_rc2 X86"
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

                if(fs.get("/solaris.zlib")==null || fs.get("/jack")==null)
                    throw error(iso+" doesn't look like an OpenSolaris CD image");

                FileEntry menu = fs.get("/boot/grub/menu.lst");
                if(menu==null)
                    throw error(iso+" doesn't look like an OpenSolaris CD image (no GRUB)");

                String menuList = IOUtils.toString(menu.read());
                Matcher m = RELEASE.matcher(menuList);
                if(m.find())
                    return m.group(1);

                throw error(iso+" doesn't contain OpenSolaris grub menu");
            } finally {
                if(fs!=null)
                    fs.close();
            }
        }
    }


    private static final Pattern RELEASE = Pattern.compile("title (.+)\n");

    private static final Logger LOGGER = Logger.getLogger(OpenSolarisBootConfiguration.class.getName());
}
