import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.Inet4Address;
import java.util.Enumeration;

/**
 * @author Kohsuke Kawaguchi
 */
public class ListInterface {
    public static void main(String[] args) throws SocketException {
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface ni =  e.nextElement();
//            if(ni.isLoopback())     continue;
//            if(ni.isPointToPoint()) continue;

            Enumeration<InetAddress> adrs = ni.getInetAddresses();
            while (adrs.hasMoreElements()) {
                InetAddress a =  adrs.nextElement();
                if (!(a instanceof Inet4Address))
                    continue;
                if(a.isLoopbackAddress())
                    continue;

                String n = ni.getDisplayName();
                if(n==null) n=ni.getName();
                System.out.println(a+" on "+ n);
            }
        }
    }
}
