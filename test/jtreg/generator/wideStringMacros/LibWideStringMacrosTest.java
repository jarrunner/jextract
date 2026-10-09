/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static test.jextract.widestringmacros.wideStringMacros_h.*;

/*
 * @test
 * @library /lib
 * @build testlib.TestUtils
 * @summary jextract should not truncate wide and Unicode string literal macros at the first zero byte
 * @run main/othervm JtregJextract -t test.jextract.widestringmacros wideStringMacros.h
 * @build LibWideStringMacrosTest
 * @run testng/othervm --enable-native-access=ALL-UNNAMED LibWideStringMacrosTest
 */
public class LibWideStringMacrosTest {
    private static final boolean LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private static final Charset UTF16 = LE ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
    private static final Charset UTF32 = LE ? StandardCharsets.UTF_32LE : StandardCharsets.UTF_32BE;
    private static final Charset WCHAR = System.getProperty("os.name").startsWith("Windows") ? UTF16 : UTF32;

    @Test
    public void testNarrowUnchanged() {
        assertBytes(NARROW(), "plain", StandardCharsets.UTF_8);
    }

    @Test
    public void testWide() {
        assertBytes(W_SIMPLE(), "SHA256", WCHAR);
        assertBytes(W_CHAIN(), "SHA256", WCHAR);
        assertBytes(W_CONCAT(), "HashDigestLength", WCHAR);
        assertBytes(W_NON_ASCII(), "café Ω", WCHAR);
        assertBytes(W_ASTRAL(), "😀!", WCHAR);
        assertBytes(W_EMBEDDED_NUL(), "a\u0000b", WCHAR);
        assertBytes(W_ESCAPES(), "tab\there \"quoted\" back\\slash", WCHAR);
    }

    @Test
    public void testUnicode() {
        assertBytes(U16(), "utf16", UTF16);
        assertBytes(U32(), "😀", UTF32);
    }

    private static void assertBytes(MemorySegment segment, String expected, Charset charset) {
        assertEquals(segment.toArray(ValueLayout.JAVA_BYTE), (expected + "\u0000").getBytes(charset));
    }
}
