/*
 * Automatically generated by jrpcgen 1.0.7+ on 11/1/19, 1:52 PM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 *
 * This version of jrpcgen adopted by dCache project
 * See http://www.dCache.ORG for details
 */
package org.dcache.nfs.v4.xdr;

import java.io.IOException;

import org.dcache.oncrpc4j.rpc.*;
import org.dcache.oncrpc4j.xdr.*;

public class fattr4_xattr_support implements XdrAble {

    public boolean value;

    public fattr4_xattr_support() {
    }

    public fattr4_xattr_support(boolean value) {
        this.value = value;
    }

    public fattr4_xattr_support(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
            throws OncRpcException, IOException {
        xdr.xdrEncodeBoolean(value);
    }

    public void xdrDecode(XdrDecodingStream xdr)
            throws OncRpcException, IOException {
        value = xdr.xdrDecodeBoolean();
    }

}
// End of fattr4_xattr_support.java
