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
    private final String tftpAddress;

    public PXEBootProcess(PathResolver resolver, String tftpAddress) {
        this.resolver = resolver;
        this.tftpAddress = tftpAddress;
    }

    public Void call() throws IOException {
        // has to bind to the "any address" to receive packets, at least on Ubuntu
        ProxyDhcpService dhcp = new ProxyDhcpService((Inet4Address)InetAddress.getByName(tftpAddress),"pxelinux.0");
        start(dhcp);

        // serve up resources
        LOGGER.info("Starting a TFTP service");
        TFTPServer tftp = new TFTPServer(resolver);
        start(tftp);

        return null;
    }

    private void start(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static final Logger LOGGER = Logger.getLogger(PXEBootProcess.class.getName());
}
