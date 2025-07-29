/*
 * Copyright (c) 2009 - 2017 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.nfs.v4;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.dcache.nfs.ChimeraNFSException;
import org.dcache.nfs.nfsstat;
import org.dcache.nfs.status.BadHandleException;
import org.dcache.nfs.status.ClidInUseException;
import org.dcache.nfs.status.NotSuppException;
import org.dcache.nfs.v4.xdr.SETCLIENTID4res;
import org.dcache.nfs.v4.xdr.SETCLIENTID4resok;
import org.dcache.nfs.v4.xdr.bitmap4;
import org.dcache.nfs.v4.xdr.clientaddr4;
import org.dcache.nfs.v4.xdr.netaddr4;
import org.dcache.nfs.v4.xdr.nfs_argop4;
import org.dcache.nfs.v4.xdr.nfs_fh4;
import org.dcache.nfs.v4.xdr.nfs_opnum4;
import org.dcache.nfs.v4.xdr.nfs_resop4;
import org.dcache.nfs.v4.xdr.verifier4;
import org.dcache.oncrpc4j.rpc.OncRpcClient;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.dcache.oncrpc4j.rpc.net.InetSocketAddresses;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.util.Opaque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationSETCLIENTID extends AbstractNFSv4Operation {

    private static final Logger _log = LoggerFactory.getLogger(OperationSETCLIENTID.class);

    public OperationSETCLIENTID(nfs_argop4 args) {
        super(args, nfs_opnum4.OP_SETCLIENTID);
    }

    @Override
    public void process(CompoundContext context, nfs_resop4 result) throws ChimeraNFSException {

        final SETCLIENTID4res res = result.opsetclientid;

        if (context.getMinorversion() > 0) {
            throw new NotSuppException("operation SETCLIENTID4 is obsolete in 4.x, x > 0");
        }

        verifier4 verifier = _args.opsetclientid.client.verifier;
        final Opaque id = _args.opsetclientid.client.id;
        NFS4Client client = context.getStateHandler().clientByOwner(id);

        if (client == null) {
            // new client
            client = context.getStateHandler().createClient(
                    context.getRemoteSocketAddress(),
                    context.getLocalSocketAddress(),
                    context.getMinorversion(),
                    _args.opsetclientid.client.id, _args.opsetclientid.client.verifier,
                    context.getPrincipal(), false);
        } else if (!client.isConfirmed()) {

            // existing client, but not confirmed. either retry or client restarted before confirmation
            context.getStateHandler().removeClient(client);
            client = context.getStateHandler().createClient(
                    context.getRemoteSocketAddress(),
                    context.getLocalSocketAddress(),
                    context.getMinorversion(),
                    _args.opsetclientid.client.id, _args.opsetclientid.client.verifier,
                    context.getPrincipal(), false);

        } else if (!client.clientGeneratedVerifierEquals(verifier)) {

            // existing client, different verifier. Client rebooted.
            // create new record, keep the old one as required by the RFC 7530
            client = context.getStateHandler().createClient(
                    context.getRemoteSocketAddress(),
                    context.getLocalSocketAddress(),
                    context.getMinorversion(),
                    _args.opsetclientid.client.id, _args.opsetclientid.client.verifier,
                    context.getPrincipal(), false);

        } else if (client.isLeaseValid()) {

            // can't be reused, if principal have changes and client has state
            if (!client.principal().equals(context.getPrincipal()) && client.hasState()) {
                netaddr4 addr = new netaddr4(client.getRemoteAddress());
                res.status = nfsstat.NFSERR_CLID_INUSE;
                res.client_using = new clientaddr4(addr);
                throw new ClidInUseException();
            }

            client.reset();
        }

        ConnectionAuthenticator connAuth = context.getConnectionAuthenticator();
        InetSocketAddress remoteSocketAddress = context.getRpcCall().getTransport().getRemoteSocketAddress();
        if (connAuth.requiresConnectionAuthentication(remoteSocketAddress)) {
            InetSocketAddress cbAddr;
            try {
                cbAddr = InetSocketAddresses.forUaddrString(
                        _args.opsetclientid.callback.cb_location.na_r_addr);
            } catch (IllegalArgumentException | NullPointerException e) {
                cbAddr = null;
            }

            if (cbAddr != null && connAuth.mayUseSecondaryPortForVerification(remoteSocketAddress, cbAddr)) {
                boolean verified = verifyClientCallback(context, cbAddr);
                connAuth.setAuthenticated(remoteSocketAddress, verified);
            }
        }

        res.resok4 = new SETCLIENTID4resok();
        res.resok4.clientid = client.getId();
        res.resok4.setclientid_confirm = client.serverGeneratedVerifier();
        res.status = nfsstat.NFS_OK;
    }

    /**
     * Tries to verify that the calling NFS client is actually legitimate.
     * <p>
     * We connect to the clientCB port specified by the client. If that port is knowingly owned by a trusted process,
     * then we can check if that process knows about the root-inode.
     * <p>
     * If it doesn't, then we know access should be denied.
     *
     * @param context The context.
     * @param client The client.
     * @return {@code true} if verified.
     */
    private boolean verifyClientCallback(CompoundContext context, InetSocketAddress callbackSockAddr) {
        boolean verified = false;

        try (OncRpcClient rpcClient = new OncRpcClient(callbackSockAddr.getAddress(), IpProtocolType.TCP,
                callbackSockAddr
                        .getPort())) {
            RpcTransport transport;
            try {
                transport = rpcClient.connect();
            } catch (IOException e) {
                e.printStackTrace();
                transport = null;
            }

            ClientCB cb = new ClientCB(
                    transport,
                    _args.opsetclientid.callback.cb_program.value,
                    context.getMinorversion(),
                    null,
                    10,
                    new RpcAuthTypeNone());
            try {
                cb.cbPing();

                nfs_fh4 fh = new nfs_fh4(context.getFs().getRootInode().toNfsHandle());

                cb.cbGetAttr(_args.opsetclientid.callback_ident, fh, new bitmap4());
                verified = true;
            } catch (BadHandleException e) {
                _log.info("Client back channel does not know about root inode: {}", e.getMessage(), e);
            } catch (Exception e) {
                _log.info("Can't reach client over back channel: {}", e.getMessage(), e);
            }
        } catch (IOException e1) {
            _log.info("Problem closing _args: {}", e1.getMessage(), e1);
        }

        return verified;
    }
}
