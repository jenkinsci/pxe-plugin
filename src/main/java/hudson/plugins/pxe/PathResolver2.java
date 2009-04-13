package hudson.plugins.pxe;

import org.jvnet.hudson.tftpd.PathResolver;
import org.jvnet.hudson.tftpd.Data;

import java.io.InputStream;
import java.io.IOException;

/**
 * {@link PathResolver} contract exposed in a slightly different way to make it remoting friendly.
 *
 * @author Kohsuke Kawaguchi
 */
interface PathResolver2 extends PathResolver {
    /**
     * The same as {@link Data#size()}.
     */
    int size(String path) throws IOException;
    /**
     * The same as {@link Data#read()}.
     */
    InputStream read(String path) throws IOException;
}
