/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
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
package org.dcache.nfs.v4.xdr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.dcache.oncrpc4j.util.Opaque;
import org.junit.Test;

public class sessionid4Test {

    @Test
    public void testEqualsTrue() {
        Opaque id = Opaque.forUtf8Bytes("bla");
        sessionid4 session1 = new sessionid4(id);
        sessionid4 session2 = new sessionid4(id);

        assertTrue("equlas sessions not recognized", session1.equals(session2));
        assertTrue("equlas sessions sould have the same hashCode",
                session1.hashCode() == session2.hashCode());
    }

    @Test
    public void testEqualsFalse() {
        Opaque id1 = Opaque.forUtf8Bytes("bla");
        Opaque id2 = Opaque.forUtf8Bytes("blabla");
        sessionid4 session1 = new sessionid4(id1);
        sessionid4 session2 = new sessionid4(id2);

        assertFalse("not equal sessions not recognized", session1.equals(session2));
    }

}
