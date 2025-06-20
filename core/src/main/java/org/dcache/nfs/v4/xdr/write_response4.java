/*
 * Automatically generated by jrpcgen 1.0.7+ on 4/3/20, 4:01 PM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 *
 * This version of jrpcgen adopted by dCache project
 * See http://www.dCache.ORG for details
 */
package org.dcache.nfs.v4.xdr;

import java.io.IOException;

import org.dcache.oncrpc4j.rpc.*;
import org.dcache.oncrpc4j.rpc.net.*;
import org.dcache.oncrpc4j.xdr.*;

public class write_response4 implements XdrAble {
    public stateid4[] wr_callback_id;
    public length4 wr_count;
    public int wr_committed;
    public verifier4 wr_writeverf;

    public write_response4() {
    }

    public write_response4(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
            throws OncRpcException, IOException {
        {
            int $size = wr_callback_id.length;
            xdr.xdrEncodeInt($size);
            for (int $idx = 0; $idx < $size; ++$idx) {
                wr_callback_id[$idx].xdrEncode(xdr);
            }
        }
        wr_count.xdrEncode(xdr);
        xdr.xdrEncodeInt(wr_committed);
        wr_writeverf.xdrEncode(xdr);
    }

    public void xdrDecode(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        {
            int $size = xdr.xdrDecodeInt();
            wr_callback_id = new stateid4[$size];
            for (int $idx = 0; $idx < $size; ++$idx) {
                wr_callback_id[$idx] = new stateid4(xdr);
            }
        }
        wr_count = new length4(xdr);
        wr_committed = xdr.xdrDecodeInt();
        wr_writeverf = new verifier4(xdr);
    }

}
// End of write_response4.java
