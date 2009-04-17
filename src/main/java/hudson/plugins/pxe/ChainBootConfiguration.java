package hudson.plugins.pxe;

import hudson.Extension;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Chain boot from another TFTP server.
 * 
 * @author Kohsuke Kawaguchi
 */
public class ChainBootConfiguration extends BootConfiguration {
    public final String hostName;
    public final String bootFile;

    @DataBoundConstructor
    public ChainBootConfiguration(String hostName, String bootFile) {
        this.hostName = hostName;
        this.bootFile = bootFile;
    }

    protected String getIdSeed() {
        return "chain";
    }

    public String getPxeLinuxConfigFragment() throws IOException {
        return String.format("LABEL %1$s\n" +
                "    MENU LABEL Chainboot from %2$s\n" +
                "    KERNEL pxechain.com\n" +
                "    APPEND %3$s::%4$s \n",
                getId(), hostName, InetAddress.getByName(hostName).getHostAddress(), bootFile);
    }

    public String getDisplayName() {
        return "Chain boot from "+hostName+":"+bootFile;
    }

    @Extension
    public static class DescriptorImpl extends BootConfigurationDescriptor {
        public String getDisplayName() {
            return "Chainboot from another TFTP server";
        }

        public FormValidation doCheckHostName(@QueryParameter String value) throws IOException {
            try {
                InetAddress.getByName(value).getHostAddress();
                return FormValidation.ok();
            } catch (UnknownHostException e) {
                return FormValidation.error(value+" doesn't look like a valid host name");
            }
        }
    }
}
