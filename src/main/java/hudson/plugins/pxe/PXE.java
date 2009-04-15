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
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.Inet4Address;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.InetAddress;

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

    public String getTftpAddress() {
        return getPlugin().getTftpAddress();
    }

    /**
     * All registered descriptors exposed for UI
     */
    public Collection<BootConfigurationDescriptor> getDescriptors() {
        return BootConfiguration.all();
    }

    public DescribableList<BootConfiguration, BootConfigurationDescriptor> getBootConfigurations() {
        return getPlugin().getBootConfigurations();
    }

    /**
     * Looks up {@link BootConfiguration} by its {@linkplain BootConfiguration#getId() id}.
     *
     * Primarily used to bind them to URL.
     */
    public BootConfiguration getConfiguration(String id) {
        for (BootConfiguration config : getBootConfigurations())
            if(config.getId().equals(id))
                return config;
        return null;
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

    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        JSONObject form = req.getSubmittedForm();

        // persist the plugin setting
        PluginImpl plugin = getPlugin();
        BulkChange bc = new BulkChange(plugin);
        try {
            plugin.setRootAccount(form.getString("rootUserName"),Secret.fromString(form.getString("rootPassword")));
            plugin.setTftpAddress(form.getString("tftpAddress"));
            getBootConfigurations().rebuildHetero(req,form,getDescriptors(),"configuration");
        } catch (FormException e) {
            throw new ServletException(e);
        } finally {
            bc.commit();
        }

        plugin.restartPXE();

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

    /**
     * Descriptor is only used for UI form bindings
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<PXE> {
        public String getDisplayName() {
            return null; // unused
        }
    }

    public static final class NIC {
        public final NetworkInterface ni;
        public final Inet4Address adrs;

        public NIC(NetworkInterface ni, Inet4Address adrs) {
            this.ni = ni;
            this.adrs = adrs;
        }

        public String getName() {
            String n = ni.getDisplayName();
            if(n==null) n=ni.getName();
            return String.format("%s (%s)",
                    adrs.getHostAddress(),n);
        }
    }

    /**
     * Because DHCP proxy doesn't know which interface the DHCP request was received,
     * it cannot determine by itself what IP address the PXE client can use to reach us.
     *
     * <p>
     * This method lists all the interfaces 
     */
    public List<NIC> getNICs() throws SocketException {
        List<NIC> r = new ArrayList<NIC>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface ni =  e.nextElement();
//            require JDK6
//            if(ni.isLoopback())     continue;
//            if(ni.isPointToPoint()) continue;

            Enumeration<InetAddress> adrs = ni.getInetAddresses();
            while (adrs.hasMoreElements()) {
                InetAddress a =  adrs.nextElement();
                if(a.isLoopbackAddress())
                    continue;
                if (a instanceof Inet4Address)
                    r.add(new NIC(ni,(Inet4Address)a));
            }
        }

        return r;
    }
}
