/*
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
package org.dcache.nfs.vfs;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.dcache.oncrpc4j.util.Opaque;
import org.junit.Test;

import com.google.common.io.BaseEncoding;

/**
 *
 */
public class FileHandleTest {

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyHandle() {
        Inode.forNfsHandle(Opaque.EMPTY_OPAQUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadVersion() {
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(
                "02caffee00000000ea15b996002e303a494e4f44453a3030303043333732333331373433393234353645423833453434383434453844323844363a30");
        Inode.forNfsHandle(Opaque.forImmutableBytes(bytes));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadMagic() {
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(
                "0100000000000000ea15b996002e303a494e4f44453a3030303043333732333331373433393234353645423833453434383434453844323844363a30");
        Inode.forNfsHandle(Opaque.forImmutableBytes(bytes));
    }

    @Test
    public void testValidHandleV1() {
        byte[] bytes = BaseEncoding.base16().lowerCase().decode(
                "01caffee00000000ea15b996002e303a494e4f44453a3030303043333732333331373433393234353645423833453434383434453844323844363a30");
        Inode fh = Inode.forNfsHandle(Opaque.forImmutableBytes(bytes));

        assertEquals(1, fh.handleVersion());
        assertEquals(0xCAFFEE, fh.getMagic());
        assertEquals(0, fh.getGeneration());
        Opaque opaque = fh.getFileIdKey();
        assertEquals("/export/data".hashCode(), fh.exportIndex());
        assertEquals("0:INODE:0000C37233174392456EB83E44844E8D28D6:0", new String(opaque.toBytes(), US_ASCII));
    }

    @Test
    public void testFileHandleConstructor() {
        Inode inode = Inode.forFileIdKey(0, "/export/data".hashCode(), 0, Opaque.forUtf8Bytes("0:INODE:0000C37233174392456EB83E44844E8D28D6:0"));

        assertArrayEquals(BaseEncoding.base16().lowerCase().decode(
                "01caffee00000000ea15b996002e303a494e4f44453a3030303043333732333331373433393234353645423833453434383434453844323844363a30"),
                inode.toNfsHandle().toBytes());
    }
}
