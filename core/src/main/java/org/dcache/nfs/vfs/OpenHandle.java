package org.dcache.nfs.vfs;

import org.dcache.oncrpc4j.util.Opaque;

/**
 * Describes metadata determined upon opening the resource at the NFS level.
 * <p>
 * This is used, for example, to infer access privileges that were determined upon opening a resource, so read/write
 * operations don't have to check every time.
 * 
 * @see PseudoFs
 */
public interface OpenHandle {
    /**
     * Returns an opaque bytes representation of the handle.
     * 
     * @return The opaque.
     */
    Opaque getOpaque();

    /**
     * Returns the clientId for the handle.
     * 
     * @return The clientId.
     */
    long getClientId();

    /**
     * Returns the sequenceId for the handle.
     * 
     * @return The sequenceId.
     */
    int getSequenceId();
}
