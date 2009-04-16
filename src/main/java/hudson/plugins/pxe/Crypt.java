package hudson.plugins.pxe;

import com.trilead.ssh2.crypto.digest.MD5;

/**
 * Partial reimplementation of libc crypt(3) for the case that involves in 
 */
public class Crypt {
    public static void main(String[] args) {
        String salt = "abcdefgh";
        String key = "abc";
        System.out.println(cryptMD5(salt, key));
    }

    public static String cryptMD5(String salt, String key) {
        MD5 md5 = new MD5();
        md5.update(key.getBytes());
        md5.update("$1$".getBytes());
        md5.update(salt.getBytes());

        MD5 alt = new MD5();
        alt.update(key.getBytes());
        alt.update(salt.getBytes());
        alt.update(key.getBytes());
        byte[] buf = new byte[16];
        alt.digest(buf);

        // each key char, add one char from alternate sum
        int i=key.length();
        for( ; i>16; i-=16)
            md5.update(buf);
        md5.update(buf,0,i);

//        /* The original implementation now does something weird: for every 1
//           bit in the key the first 0 is added to the buffer, for every 0
//           bit the first character of the key.  This does not seem to be
//           what was intended but we have to follow this to be compatible.  */
//        for (cnt = key_len; cnt > 0; cnt >>= 1)
//          __md5_process_bytes ((cnt & 1) != 0 ? (const char *) alt_result : key, 1,
//                               &ctx);
//
        for( i=key.length(); i>0; i>>=1 )
            md5.update( ((i&1)!=0 ? "\0" : key.substring(0,1)).getBytes());

        byte[] tmp = new byte[16];
        md5.digest(tmp);

        for (i=0; i<1000; i++) {
            md5.reset();
            if(i%2 != 0)
                md5.update(key.getBytes());
            else
                md5.update(tmp);
            if(i%3 != 0)
                md5.update(salt.getBytes());
            if(i%7 != 0)
                md5.update(key.getBytes());
            if(i%2 != 0)
                md5.update(tmp);
            else
                md5.update(key.getBytes());
            md5.digest(tmp);
        }

        return "$1$"+salt+"$"
                +b64(tmp[0], tmp[6], tmp[12],4)
                +b64(tmp[1], tmp[7], tmp[13],4)
                +b64(tmp[2], tmp[8], tmp[14],4)
                +b64(tmp[3], tmp[9], tmp[15],4)
                +b64(tmp[4], tmp[10], tmp[5],4)
                +b64(0,0, tmp[11],2);
    }

    /**
     * crypt(3) uses a modified base64
     */
    private static final String CODEC = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static String b64(int b2, int b1, int b0, int n) {
        String r ="";
        b2&=0xFF;
        b1&=0xFF;
        b0&=0xFF;

        int w = (b2<<16)|(b1<<8)|b0;
        for( ; n>0; n-- ) {
            r+=CODEC.charAt(w&0x3F);
            w>>=6;
        }
        return r;
    }
}
