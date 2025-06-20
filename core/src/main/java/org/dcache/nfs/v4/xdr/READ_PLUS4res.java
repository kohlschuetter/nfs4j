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

import org.dcache.nfs.nfsstat;
import org.dcache.oncrpc4j.rpc.*;
import org.dcache.oncrpc4j.rpc.net.*;
import org.dcache.oncrpc4j.xdr.*;

public class READ_PLUS4res implements XdrAble {
    public int rp_status;
    public read_plus_res4 rp_resok4;

    public READ_PLUS4res() {
    }

    public READ_PLUS4res(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
            throws OncRpcException, IOException {
        xdr.xdrEncodeInt(rp_status);
        switch (rp_status) {
            case nfsstat.NFS_OK:
                rp_resok4.xdrEncode(xdr);
                break;
            default:
                break;
        }
    }

    public void xdrDecode(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        rp_status = xdr.xdrDecodeInt();
        switch (rp_status) {
            case nfsstat.NFS_OK:
                rp_resok4 = new read_plus_res4(xdr);
                break;
            default:
                break;
        }
    }

}
// End of READ_PLUS4res.java
