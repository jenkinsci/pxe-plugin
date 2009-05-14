package hudson.plugins.pxe;

import hudson.model.Hudson;
import hudson.util.FormValidation;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Convenient partial {@link BootConfiguration} implementation that uses
 * a zip file as the backend of the image storage.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ZipBasedBootConfiguration extends BootConfiguration {
    /**
     * Location of the ZIP image file.
     */
    public final File zip;

    private volatile String release;

    protected ZipBasedBootConfiguration(File zip) {
        this.zip = zip;
    }

    public String getRelease() {
        if(release==null)
            try {
                ZipFile zip = new ZipFile(this.zip);
                release = getDescriptor().getReleaseInfo(zip);
                zip.close();
            } catch (IOException e) {
                release = "Broken image at "+ zip;
            }
        return release;
    }

    public ZipTree doImage() {
        return new ZipTree(zip);
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        rsp.sendRedirect("./image/");
    }

    public String getDisplayName() {
        return getRelease();
    }

    @Override
    public ZipBasedBootConfigurationDescriptor getDescriptor() {
        return (ZipBasedBootConfigurationDescriptor)super.getDescriptor();
    }

    public static abstract class ZipBasedBootConfigurationDescriptor extends BootConfigurationDescriptor {
        public FormValidation doCheckZip(@QueryParameter String value) throws IOException {
            // insufficient permission to perform validation?
            if(!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) return ok();

            if(value.trim().length()==0)    return ok();    // nothing entered yet

            File f = new File(value);
            if(!f.exists())
                return error("No such file file exists: "+value);

            ZipFile zip=null;
            try {
                try {
                    zip = new ZipFile(f);
                } catch (IOException e) {
                    return error(value+" doesn't look like a valid zip file");
                }

                try {
                    return ok(getReleaseInfo(zip));
                } catch (FormValidation e) {
                    return e;
                }
            } finally {
                if(zip!=null)
                    zip.close();
            }
        }

        /**
         * Obtain the identifier that represents a release information.
         *
         * @throws FormValidation
         *      if the file isn't the expected file.
         */
        protected abstract String getReleaseInfo(ZipFile zip) throws IOException;
    }
}
