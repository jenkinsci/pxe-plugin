package hudson.plugins.pxe;

import hudson.Extension;
import hudson.Util;
import hudson.model.Hudson;
import static hudson.util.FormValidation.error;
import org.apache.commons.io.IOUtils;
import org.kohsuke.loopy.FileEntry;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ubuntu boot configuration.
 *
 * @author Kohsuke Kawaguchi
 */
public class UbuntuBootConfiguration extends LinuxBootConfiguration {
    // preseed configurations that are user configurable
    public final String additionalPackages;
    public final String userName;
    public final String password;
    public final String lateCommand;
    public final String customMirror;
    /**
     * User-defined preseed content.
     * Useful to override default settings
     */
    public final String userContent;

    @DataBoundConstructor
    public UbuntuBootConfiguration(File iso, String userName, String password, String additionalPackages, String lateCommand, String customMirror, String userContent) {
        super(iso);

        if(Util.fixEmptyAndTrim(userName)==null)    userName="hudson";
        this.userName = userName;

        if(Util.fixEmptyAndTrim(password)==null)    password="hudson";
        if(!password.startsWith("$1$"))
            password = Crypt.cryptMD5("abcdefgh",password);
        this.password = password;

        this.additionalPackages = additionalPackages;
        
        if(lateCommand!=null && !lateCommand.startsWith("#!"))
            lateCommand = "#!/bin/bash\n"+lateCommand;
        this.lateCommand = lateCommand;

        this.customMirror = Util.fixEmpty(customMirror);
        this.userContent = Util.fixEmpty(userContent);
    }

    protected String getIdSeed() {
        // try to extract a short name from the release information
        Pattern p = Pattern.compile("Ubuntu[^ ]* ([0-9.]+).+?(i386|amd64)?");
        Matcher m = p.matcher(getRelease());
        if(m.find()) {
            if(m.group(2)!=null)    return "ubuntu"+m.group(1)+'.'+m.group(2);
            else                    return "ubuntu"+m.group(1);
        }
        return "ubuntu";
    }

    protected FileEntry getTftpIsoMountDir(ISO9660FileSystem fs) throws IOException {
        FileEntry installer = fs.get("/install/netboot/ubuntu-installer");
        if(installer==null) throw new IOException("/install/netboot/ubuntu-installer not found on "+iso);
        LinkedHashMap<String,FileEntry> children = installer.childEntries();
        FileEntry arch = children.get("i386");
        if(arch==null)  arch=children.get("amd64");
        if(arch==null)      throw new IOException("/install/netboot/ubuntu-installer/(amd64|i386) not found on "+iso);
        return arch;
    }

    /**
     * Serves the preseed file
     */
    public void doPreseed(StaplerResponse rsp) throws IOException {
        serveMacroExpandedResource(rsp,"preseed.txt");
    }
    
//
// preseeding configuration
//
    /**
     * Ubuntu repository mirror, meaning the ISO image.
     */
    public String getMirrorHostName() throws IOException {
        URL url = getMirrorUrl();
        if(url.getPort()!=80 && url.getPort()!=-1)
            return url.getHost()+':'+url.getPort();
        return url.getHost();
    }

    public String getMirrorDirectory() throws IOException {
        return getMirrorUrl().getPath();
    }

    private URL getMirrorUrl() throws MalformedURLException {
        return new URL(customMirror!=null ? customMirror : Hudson.getInstance().getRootUrl()+"pxe/"+getUrl()+"/image");
    }

    public void doLateCommand(StaplerResponse rsp) throws IOException {
        rsp.setContentType("text/plain;charset=UTF-8");
        PrintWriter w = rsp.getWriter();
        w.print(Util.fixNull(lateCommand));
        w.close();
    }

    @Extension
    public static class DescriptorImpl extends IsoBasedBootConfigurationDescriptor {
        public String getDisplayName() {
            return "Ubuntu";
        }

        /**
         * This returns string like "Ubuntu-Server 8.10 "Intrepid Ibex" - Release i386 (20081028.1)"
         */
        protected String getReleaseInfo(File iso) throws IOException {
            ISO9660FileSystem fs=null;
            try {
                try {
                    fs = new ISO9660FileSystem(iso,false);
                } catch (IOException e) {
                    LOGGER.log(Level.INFO,iso+" isn't an ISO file?",e);
                    throw error(iso+" doesn't look like an ISO file");
                }

                FileEntry info = fs.get("/.disk/info");
                if(info==null)
                    throw error(iso+" doesn't look like an Ubuntu CD/DVD image");

                FileEntry installer = fs.get("/install/netboot/ubuntu-installer");
                if(installer==null)
                    throw error(iso+" doesn't have the network boot installer in it. Perhaps it's a desktop CD?");

                return IOUtils.toString(info.read());
            } finally {
                if(fs!=null)
                    fs.close();
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(UbuntuBootConfiguration.class.getName());
}
