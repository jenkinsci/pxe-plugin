package hudson.plugins.pxe;

import org.kohsuke.loopy.FileEntry;
import org.kohsuke.loopy.iso9660.ISO9660FileSystem;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Binding {@link ISO9660FileSystem} to an HTTP URL space. 
 *
 * @author Kohsuke Kawaguchi
 */
public class ISO9660Tree implements HttpResponse {
    private final File iso;

    public ISO9660Tree(File iso) {
        this.iso = iso;
    }

    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
        ISO9660FileSystem fs = new ISO9660FileSystem(iso,false);
        try {
            String rest = req.getRestOfPath();
            FileEntry e = fs.get(rest);
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
                for (FileEntry f : e.childEntries().values())
                    w.printf("<LI><A HREF='%1$s%2$s'>%1$s%2$s</A></LI>",f.getName(),f.isDirectory()?"/":"");
                w.println("</body></html>");
            } else {
                InputStream in = e.read();
                rsp.serveFile(req, in, e.getLastModifiedTime(), -1, e.getSize(), e.getName() );
            }
        } finally {
            fs.close();
        }
    }
}
