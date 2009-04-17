package hudson.plugins.pxe;

import hudson.model.Hudson;
import hudson.remoting.Channel;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.VirtualChannel;
import hudson.util.DescribableList;
import org.apache.commons.io.IOUtils;
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
    public Data open(final String fileName) throws IOException {
        // dynamic resources
        DescribableList<BootConfiguration,BootConfigurationDescriptor> bootConfigurations = PXE.get().getBootConfigurations();

        for(BootConfiguration config : bootConfigurations) {
            String id = config.getId()+'/';
            if(fileName.startsWith(id)) {
                Data d = config.tftp(fileName.substring(id.length()));
                if(d!=null)     return d;
            }
        }

        if(fileName.equals("pxelinux.cfg/default")) {
            // combine all pxelinux.cfg.fragment files into one and serve them
            StringBuilder buf = new StringBuilder(IOUtils.toString(getClass().getClassLoader().getResourceAsStream("tftp/"+fileName)));
            buf.append('\n');
            
            // merge all fragments
            for (BootConfiguration conf : bootConfigurations)
                buf.append(conf.getPxeLinuxConfigFragment()).append('\n');
            return Data.from(buf.toString());
        }

        // static resources
        URL res = Hudson.getInstance().getPluginManager().uberClassLoader.getResource("tftp/"+fileName);
        if(res!=null)
            return Data.from(res);

        throw new IOException("No such file: "+fileName);
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
        return export(Channel.current());
    }

    public PathResolver export(VirtualChannel channel) {
        return new RemotePathResolverProxy(channel.export(PathResolver2.class, this));
    }

    /**
     * If {@link PathResolverImpl} is shipped to a remote JVM, this object is sent instead
     * to make everything remoting-safe.
     */
    private static final class RemotePathResolverProxy implements PathResolver, Serializable {
        private final PathResolver2 proxy;

        private RemotePathResolverProxy(PathResolver2 proxy) {
            this.proxy = proxy;
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
