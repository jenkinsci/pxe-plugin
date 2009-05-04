package hudson.plugins.pxe;

import hudson.BulkChange;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.tasks.Mailer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.os.SU;
import hudson.remoting.VirtualChannel;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.StreamTaskListener;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * This object is bound to "/pxe" and handles all the UI work.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class PXE extends ManagementLink implements StaplerProxy, Describable<PXE>, Saveable {
    private String rootUserName;
    private Secret rootPassword;
    private final DescribableList<BootConfiguration, BootConfigurationDescriptor> bootConfigurations = new DescribableList<BootConfiguration, BootConfigurationDescriptor>(this);
    /**
     * Numberic IP address of the TFTP server, like "1.2.3.4"
     */
    private String tftpAddress;

    // running state
    private transient VirtualChannel channel;
    private transient DaemonService daemonService;

    public PXE() throws IOException, InterruptedException {
        load();
        assignIDs();
        restartPXE();
    }

    public String getIconFileName() {
        return "orange-square.gif";
    }

    @Override
    public String getDescription() {
        return "Simplify the slave installation by network installing them (AKA PXE boot)";
    }

    public String getUrlName() {
        return "pxe";
    }

    public String getDisplayName() {
        return "Network Slave Installation Management";
    }

    public String getRootUserName() {
        return rootUserName;
    }

    public Secret getRootPassword() {
        return rootPassword;
    }

    public DescribableList<BootConfiguration, BootConfigurationDescriptor> getBootConfigurations() {
        return bootConfigurations;
    }

    public void setTftpAddress(String address) throws IOException {
        this.tftpAddress = address;
        save();
    }

    public String getTftpAddress() {
        return tftpAddress;
    }

    public VirtualChannel getChannel() {
        return channel;
    }

    public DaemonService getDaemonService() {
        return daemonService;
    }

    public File getLogFile() {
        return new File(Hudson.getInstance().getRootDir(),"pxe.log");
    }

    public synchronized void restartPXE() throws IOException, InterruptedException {
        if(Hudson.getInstance().getRootUrl()==null) {
            LOGGER.warning("Not starting TFTP/ProxyDHCP because Hudson Root URL is not configured");
            return;
        }
        if(tftpAddress==null) {
            LOGGER.warning("Not starting TFTP/ProxyDHCP service due to incomplete configuration");
            return;
        }

        if(channel!=null) {
            LOGGER.info("Stopping TFTP/ProxyDHCP service");
            channel.close();
            channel.join(3000);
        }
        LOGGER.info("Starting TFTP/ProxyDHCP service");
        TaskListener listener = new StreamTaskListener(getLogFile());
        channel = SU.start(listener, rootUserName, rootPassword == null ? null : rootPassword.toString());
        // export explicitly, or else it'll be unexported upon return
        daemonService = channel.call(new PXEBootProcess(new PathResolverImpl().export(channel), tftpAddress));
    }

    public void setRootAccount(String userName, Secret password) throws IOException {
        this.rootUserName = Util.fixEmptyAndTrim(userName);
        this.rootPassword = password;
        save();
    }

    /**
     * Obtains the status of the daemon.
     */
    public FormValidation getDaemonStatus() {
        if(Mailer.descriptor().getUrl()==null)
            return FormValidation.warningWithMarkup("<a href='../configure'>Hudson Root URL is not configured</a>.");
        DaemonService ds = getDaemonService();
        if(ds==null)
            return FormValidation.warningWithMarkup("PXE service is not yet started. <a href='console'>Check console for the status</a>");
        if(!ds.isDHCPProxyAlive())
            return FormValidation.errorWithMarkup("DHCP proxy service is failing. <a href='console'>Check console for the status</a>");
        if(!ds.isTFTPAlive())
            return FormValidation.error("TFTP service is failing");
        return FormValidation.ok();
    }

    /**
     * All registered descriptors exposed for UI
     */
    public Collection<BootConfigurationDescriptor> getDescriptors() {
        return BootConfiguration.all();
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

    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException, InterruptedException {
        JSONObject form = req.getSubmittedForm();

        // persist the setting
        BulkChange bc = new BulkChange(this);
        try {
            setRootAccount(form.getString("rootUserName"),Secret.fromString(form.getString("rootPassword")));
            if(form.has("tftpAddress"))
                setTftpAddress(form.getString("tftpAddress"));
            else
                setTftpAddress(getNICs().get(0).adrs.getHostAddress());
            for (BootConfiguration c : bootConfigurations)
                c.shutdown();
            bootConfigurations.rebuildHetero(req,form,getDescriptors(),"configuration");
            assignIDs();
        } catch (FormException e) {
            throw new ServletException(e);
        } finally {
            bc.commit();
        }

        restartPXE();

        rsp.sendRedirect(".");
    }

    private void assignIDs() {
        // recompute IDs
        DescribableList<BootConfiguration, BootConfigurationDescriptor> all = getBootConfigurations();
        OUTER:
            for (BootConfiguration a : all) {
            String seed = a.getIdSeed();
            for( BootConfiguration b : all) {
                if(b==a) continue;
                if(b.getIdSeed().equals(seed)) {
                    // conflict. resolve by adding index
                    int index=1;
                    for (BootConfiguration c : all) {
                        if(c==a) {
                            a.id=seed+"_"+index;
                            continue OUTER;
                        }
                        if(c.getIdSeed().equals(seed))
                            index++;
                    }
                    throw new AssertionError();
                }
            }
            // no conflict
            a.id=seed;
        }
    }

    /**
     * Handles incremental log output.
     */
    public void doProgressiveLog( StaplerRequest req, StaplerResponse rsp) throws IOException {
        new LargeText(getLogFile(),false).doProgressText(req,rsp);
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

    protected void load() throws IOException {
        XmlFile xml = getConfigXml();
        if(xml.exists())
            xml.unmarshal(this);
    }

    public void save() throws IOException {
        if(BulkChange.contains(this))   return;
        getConfigXml().write(this);
    }

    protected XmlFile getConfigXml() {
        return new XmlFile(Hudson.XSTREAM,
                new File(Hudson.getInstance().getRootDir(),"pxe.xml"));
    }

    public static PXE get() {
        return ManagementLink.all().get(PXE.class);
    }

    private static final Logger LOGGER = Logger.getLogger(PXE.class.getName());
}
