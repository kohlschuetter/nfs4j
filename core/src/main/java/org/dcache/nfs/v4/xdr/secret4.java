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

public class secret4 implements XdrAble {

    public String value;

    public secret4() {
    }

    public secret4(String value) {
        this.value = value;
    }

    public secret4(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
            throws OncRpcException, IOException {
        xdr.xdrEncodeString(value);
    }

    public void xdrDecode(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        value = xdr.xdrDecodeString();
    }

}
// End of secret4.java
