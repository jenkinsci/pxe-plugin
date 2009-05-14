package hudson.plugins.pxe;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.loopy.FileEntry;
import org.jvnet.hudson.tftpd.Data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import hudson.util.FormValidation;
import static hudson.util.FormValidation.ok;
import static hudson.util.FormValidation.error;
import hudson.model.Hudson;

/**
 * Convenient partial {@link BootConfiguration} implementation that uses
 * an ISO file as the backend of the image storage.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class IsoBasedBootConfiguration extends BootConfiguration {
    /**
     * Location of the CD/DVD image file.
     */
    public final File iso;

    private volatile String release;

    protected IsoBasedBootConfiguration(File iso) {
        this.iso = iso;
    }

    public String getRelease() {
        if(release==null)
            try {
                release = getDescriptor().getReleaseInfo(iso);
            } catch (IOException e) {
                release = "Broken image at "+iso;
            }
        return release;
    }

    public ISO9660Tree doImage() {
        return new ISO9660Tree(iso);
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        rsp.sendRedirect("./image/");
    }

    public String getDisplayName() {
        return getRelease();
    }

    @Override
    public IsoBasedBootConfigurationDescriptor getDescriptor() {
        return (IsoBasedBootConfigurationDescriptor)super.getDescriptor();
    }

    /**
     * {@link Data} wrapper for {@link FileEntry}.
     */
    protected static final class FileEntryData extends Data {
        private final FileEntry data;

        public FileEntryData(FileEntry entry) {
            this.data = entry;
        }

        public InputStream read() throws IOException {
            return data.read();
        }

        @Override
        public int size() throws IOException {
            return data.getSize();
        }
    }

    public static abstract class IsoBasedBootConfigurationDescriptor extends BootConfigurationDescriptor {
        public FormValidation doCheckIso(@QueryParameter String value) throws IOException {
            // insufficient permission to perform validation?
            if(!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) return ok();

            if(value.trim().length()==0)    return ok();    // nothing entered yet

            File f = new File(value);
            if(!f.exists())
                return error("No such file file exists: "+value);

            try {
                return ok(getReleaseInfo(f));
            } catch (FormValidation e) {
                return e;
            }
        }

        /**
         * Obtain the identifier that represents a release information.
         *
         * @throws FormValidation
         *      if the file isn't the expected file.
         */
        protected abstract String getReleaseInfo(File iso) throws IOException;
    }
}
