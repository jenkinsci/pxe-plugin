package hudson.plugins.pxe;

/**
 * Represents the interface to the daemon services running in the SU environment.
 *
 * @author Kohsuke Kawaguchi
 */
public interface DaemonService {
    public boolean isDHCPProxyAlive();
    public boolean isTFTPAlive();
}
