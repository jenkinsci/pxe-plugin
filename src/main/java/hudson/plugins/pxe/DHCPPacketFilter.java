package hudson.plugins.pxe;

/**
 * @author Kohsuke Kawaguchi
 */
public interface DHCPPacketFilter {
    /**
     * Given 16 bytes hardware address, determine if the proxy DHCP service will respond.
     */
    boolean shallWeRespond(byte[] address);
}
