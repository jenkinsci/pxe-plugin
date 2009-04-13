package hudson.plugins.pxe;

import hudson.remoting.Callable;
import hudson.util.IOException2;
import org.jvnet.hudson.proxy_dhcp.ProxyDhcpService;
import org.jvnet.hudson.pxe.PXEBooter;
import org.jvnet.hudson.tftpd.Data;
import org.jvnet.hudson.tftpd.PathResolver;
import org.jvnet.hudson.tftpd.TFTPServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * This runs DHCP proxy service and TFTP service in a separate JVM that has the higher priviledge.
 * This callable blocks forever.
 *
 * @author Kohsuke Kawaguchi
 */
public class PXEBootProcess implements Callable<Void, IOException> {
    private final PathResolver resolver;

    public PXEBootProcess(PathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * List up addresses that we should be listening to.
     */
    public List<Inet4Address> getAddresses() throws SocketException {
        List<Inet4Address> r = new ArrayList<Inet4Address>();
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
                    r.add((Inet4Address)a);
            }
        }

        return r;
    }

    public Void call() throws IOException {
        // start DHCP proxying on all the interfaces separately, so that each one knows what IP to send to
        // direct PXE clients as TFTP server.
        for (Inet4Address a : getAddresses()) {
            LOGGER.info("Starting a proxy DHCP service on "+a);
            ProxyDhcpService dhcp = new ProxyDhcpService(a,"pxelinux.0",a);
            start(dhcp);
        }

        // serve up resources
        LOGGER.info("Starting a TFTP service");
        TFTPServer tftp = new TFTPServer(resolver);
        start(tftp);

        return null;
    }

    private void hang() throws IOException {
        try {
            Object o = new Object();
            synchronized (o) {
                o.wait();
            }
        } catch (InterruptedException e) {
            throw new IOException2("interrupted",e);
        }
    }

    private void start(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static final Logger LOGGER = Logger.getLogger(PXEBootProcess.class.getName());
}
