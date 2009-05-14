package hudson.plugins.pxe;

import hudson.Extension;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jvnet.hudson.tftpd.Data;

/**
 * @author Kohsuke Kawaguchi
 */
public class PartedMagicConfiguration extends ZipBasedBootConfiguration {
    @DataBoundConstructor
    public PartedMagicConfiguration(File zip) {
        super(zip);
    }

    @Override
    protected String getIdSeed() {
        return "partedMagic";
    }

    @Override
    public String getPxeLinuxConfigFragment() throws IOException {
        return String.format("LABEL %1$s\n" +
                "    MENU LABEL %2$s\n" +
                "    KERNEL %1$s/bzImage\n" +
                "    APPEND initrd=%1$s/initramfs load_ramdisk=1 prompt_ramdisk=0 rw sleep=10\n",
                getId(), getRelease());
    }

    @Override
    public Data tftp(String fileName) throws IOException {
        final ZipFile zip = new ZipFile(this.zip);
        Enumeration e = zip.entries();
        while (e.hasMoreElements()) {
            final ZipEntry ze = (ZipEntry) e.nextElement();
            if(ze.getName().endsWith('/'+fileName))
                return new Data() {
                    @Override
                    public int size() throws IOException {
                        return (int)ze.getSize();
                    }

                    @Override
                    public InputStream read() throws IOException {
                        return zip.getInputStream(ze);
                    }
                };
        }
        return null;
    }

    @Extension
    public static class DescriptorImpl extends ZipBasedBootConfigurationDescriptor {
        public String getDisplayName() {
            return "Parted Magic";
        }

        /**
         * This is like "Parted Magic 4.1"
         */
        protected String getReleaseInfo(ZipFile zip) throws IOException {
            Enumeration e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                Matcher m = VERSION.matcher(ze.getName());
                if(m.find())
                    return "Parted Magic "+m.group(1);
            }
            throw FormValidation.error(zip.getName()+" doesn't contain Parted Magic PXE files");
        }

        private static Pattern VERSION = Pattern.compile("pmagic-pxe-([0-9.]+)");
    }
}
