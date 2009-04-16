package hudson.plugins.pxe;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import org.jvnet.hudson.proxy_dhcp.ProxyDhcpService;
import org.jvnet.hudson.tftpd.PathResolver;
import org.jvnet.hudson.tftpd.TFTPServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * This runs DHCP proxy service and TFTP service in a separate JVM that has the higher priviledge.
 * This callable blocks forever.
 *
 * @author Kohsuke Kawaguchi
 */
public class PXEBootProcess implements Callable<DaemonService, IOException> {
    private final PathResolver resolver;
    private final String tftpAddress;

    public PXEBootProcess(PathResolver resolver, String tftpAddress) {
        this.resolver = resolver;
        this.tftpAddress = tftpAddress;
    }

    public DaemonService call() throws IOException {
        // has to bind to the "any address" to receive packets, at least on Ubuntu
        LOGGER.info("Starting a DHCP proxy service");
        final Thread dhcp = start(new ProxyDhcpService((Inet4Address)InetAddress.getByName(tftpAddress),"pxelinux.0"));

        // serve up resources
        LOGGER.info("Starting a TFTP service");
        final Thread tftp = start(new TFTPServer(resolver));

        LOGGER.info("All services ready");
        return Channel.current().export(DaemonService.class,new DaemonService() {
            public boolean isDHCPProxyAlive() {
                return dhcp.isAlive();
            }

            public boolean isTFTPAlive() {
                return tftp.isAlive();
            }
        });
    }

    private Thread start(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static final Logger LOGGER = Logger.getLogger(PXEBootProcess.class.getName());
}
