package hudson.plugins.pxe;

import hudson.model.Hudson;
import hudson.remoting.Channel;
import hudson.remoting.RemoteInputStream;
import org.jvnet.hudson.tftpd.Data;
import org.jvnet.hudson.tftpd.PathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;

/**
 * {@link PathResolver} implementation that projects the TFTP storage.
 *
 * <p>
 * The implementation is remoting safe.
 * 
 * @author Kohsuke Kawaguchi
 */
final class PathResolverImpl implements PathResolver2, Serializable {
    public Data open(String fileName) throws IOException {
        // static resource first
        URL res = Hudson.getInstance().getPluginManager().uberClassLoader.getResource("tftp/"+fileName);
        if(res!=null)
            return Data.fromURL(res);

        // dynamic resources
        for(BootConfiguration config : Hudson.getInstance().getPlugin(PluginImpl.class).getBootConfigurations()) {
            Data d = config.tftp(fileName);
            if(d!=null)     return d;
        }

        // TODO: combined fragments --- pxelinux.0/default

        return null;
    }

    public int size(String path) throws IOException {
        return open(path).size();
    }

    public InputStream read(String path) throws IOException {
        return new RemoteInputStream(open(path).read());
    }

    /**
     * When sent to the remote JVM, send a proxy.
     */
    private Object writeReplace() {
        return new RemotePathResolverProxy(this);
    }

    /**
     * If {@link PathResolverImpl} is shipped to a remote JVM, this object is sent instead
     * to make everything remoting-safe.
     */
    private static final class RemotePathResolverProxy implements PathResolver, Serializable {
        private final PathResolver2 proxy;

        private RemotePathResolverProxy(PathResolver2 proxy) {
            this.proxy = Channel.current().export(PathResolver2.class, proxy);
        }

        public Data open(final String fileName) throws IOException {
            return new Data() {
                public InputStream read() throws IOException {
                    return proxy.read(fileName);
                }

                public int size() throws IOException {
                    return proxy.size(fileName);
                }
            };
        }

        private static final long serialVersionUID = 1L;
    }
}
