package hudson.plugins.pxe;

import org.apache.commons.codec.binary.Hex;

import java.text.ParseException;

/**
 * MAC address, formatted like "0A:00:B3:12:34:56".
 *
 * @author Kohsuke Kawaguchi
 */
public final class MacAddress {
    private final String id;

    public MacAddress(byte[] adrs) {
        String hex = new String(Hex.encodeHex(adrs)).substring(0, 12);
        id = reformat(hex);
    }

    private String reformat(String hex) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<6; i++) {
            if (buf.length()>0) buf.append(':');
            buf.append(hex.substring(i*2,i*2+2));
        }
        return buf.toString().toUpperCase();
    }

    public MacAddress(String adrs) throws ParseException {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<adrs.length(); i++) {
            char ch = Character.toUpperCase(adrs.charAt(i));
            if (('0'<=ch && ch<='9') || ('A'<=ch && ch<='F'))
                buf.append(ch);
        }
        if (buf.length()!=12)
            throw new ParseException("Not a valid MAC address: "+adrs,0);
        id = reformat(buf.toString());
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MacAddress that = (MacAddress) o;
        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
