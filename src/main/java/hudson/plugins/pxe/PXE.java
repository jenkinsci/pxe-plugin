package hudson.plugins.pxe;

import hudson.BulkChange;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.model.Descriptor.FormException;
import hudson.util.DescribableList;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;

/**
 * This object is bound to "/pxe" and handles all the UI work.
 *
 * <p>
 * The actual configuration is stored in {@link PluginImpl} to reuse the plugin persistence mechanism.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PXE extends ManagementLink implements StaplerProxy, Describable<PXE> {
    public String getIconFileName() {
        return "orange-square.gif";
    }

    public String getUrlName() {
        return "pxe";
    }

    public String getDisplayName() {
        return "PXE Boot management";
    }

    public String getRootUserName() {
        return getPlugin().getRootUserName();
    }

    public Secret getRootPassword() {
        return getPlugin().getRootPassword();
    }

    /**
     * All registered boot images exposed for UI.
     */
    public Collection<BootImage> getBootImages() {
        return BootImage.all();
    }

    public DescribableList<BootConfiguration,BootImage> getBootConfigurations() {
        return getPlugin().getBootConfigurations();
    }

    /**
     * Access to this object requires the admin permission.
     */
    public Object getTarget() {
        Hudson.getInstance().checkPermission(Hudson.ADMINISTER);
        return this;
    }

    /**
     * Access to the singleton {@link PluginImpl} instance.
     */
    private PluginImpl getPlugin() {
        return Hudson.getInstance().getPlugin(PluginImpl.class);
    }

    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        JSONObject form = req.getSubmittedForm();

        // persist the plugin setting
        PluginImpl plugin = getPlugin();
        BulkChange bc = new BulkChange(plugin);
        try {
            plugin.setRootAccount(form.getString("rootUserName"),Secret.fromString(form.getString("rootPassword")));
            getBootConfigurations().rebuildHetero(req,form,getBootImages(),"configuration");
        } catch (FormException e) {
            throw new ServletException(e);
        } finally {
            bc.commit();
        }

        rsp.sendRedirect(".");
    }

    /**
     * Handles incremental log output.
     */
    public void doProgressiveLog( StaplerRequest req, StaplerResponse rsp) throws IOException {
        new LargeText(getPlugin().getLogFile(),false).doProgressText(req,rsp);
    }

    public DescriptorImpl getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<PXE> {
        public String getDisplayName() {
            return null; // unused
        }
    }
}
