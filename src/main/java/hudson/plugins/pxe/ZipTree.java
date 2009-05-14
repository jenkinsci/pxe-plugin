package hudson.plugins.pxe;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Binding {@link ZipFile} to an HTTP URL space.
 *
 * <p>
 * Not designed for great performance.
 *
 * @author Kohsuke Kawaguchi
 */
public class ZipTree implements HttpResponse {
    private final File zip;

    public ZipTree(File zip) {
        this.zip = zip;
    }

    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        ZipFile zf = new ZipFile(zip);
        try {
            String rest = req.getRestOfPath().substring(1); // trim off the head '/'
            if(!rest.endsWith("/")) rest+="/";

            // because of the idiosyncrasy in zf.getEntry, it may match "foo" when it should only match "foo/",
            // so be defensive
            ZipEntry e = zf.getEntry(rest);
            if(e==null) e=zf.getEntry(rest.substring(0,rest.length()-1));
            
            if(e==null) {
                rsp.sendError(SC_NOT_FOUND);
                return;
            }

            if(e.isDirectory()) {
                // if the target page to be displayed is a directory and the path doesn't end with '/', redirect
                StringBuffer reqUrl = req.getRequestURL();
                if(reqUrl.charAt(reqUrl.length()-1)!='/') {
                    rsp.sendRedirect2(reqUrl.append('/').toString());
                    return;
                }

                rsp.setContentType("text/html");
                PrintWriter w = new PrintWriter(rsp.getWriter());
                w.println("<html><body>");
                Enumeration<? extends ZipEntry> list = zf.entries();
                while (list.hasMoreElements()) {
                    ZipEntry f =  list.nextElement();
                    if(f.getName().startsWith(e.getName()))
                        w.printf("<LI><A HREF='%1$s'>%1$s</A></LI>",f.getName().substring(rest.length()));
                }
                w.println("</body></html>");
            } else {
                InputStream in = zf.getInputStream(e);
                rsp.serveFile(req, in, e.getTime(), 1000L*1000*1000, (int)e.getSize(), e.getName());
            }
        } finally {
            zf.close();
        }
    }
}
