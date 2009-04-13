package hudson.plugins.pxe;

import org.jvnet.hudson.tftpd.PathResolver;

import java.io.InputStream;
import java.io.IOException;

/**
 * {@link PathResolver} contract exposed in a slightly different way to make it remoting friendly.
 *
 * @author Kohsuke Kawaguchi
 */
interface PathResolver2 extends PathResolver {
    int size(String path) throws IOException;
    InputStream read(String path) throws IOException;
}
