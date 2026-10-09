/*
 *  Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Decodes the spelling libclang gives a string literal cursor (clang's StringLiteral::outputString)
 * back into the Java string it denotes.
 *
 * This exists for wide and Unicode literals (L"...", u"...", U"..."): libclang's
 * clang_EvalResult_getAsStr returns the literal's bytes as a NUL-terminated C string, so a
 * UTF-16 literal such as L"SHA256" ('S', 0, 'H', 0, ...) is truncated to "S". The cursor
 * spelling, by contrast, is the fully preprocessed literal (macro references expanded, adjacent
 * literals concatenated) with every non-printable code unit written as an escape, so it can be
 * decoded exactly.
 */
final class StringLiteralDecoder {

    private StringLiteralDecoder() {}

    /**
     * Returns true if the spelling denotes a literal whose code units are wider than one byte.
     */
    static boolean isWide(String spelling) {
        return spelling.startsWith("L\"") || spelling.startsWith("u\"") || spelling.startsWith("U\"");
    }

    /**
     * Decodes a (possibly concatenated) C string literal spelling, or returns empty if the
     * spelling is not a well-formed literal.
     */
    static Optional<String> decode(String spelling) {
        boolean narrow = !isWide(spelling);
        StringBuilder wide = new StringBuilder();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int i = 0;
        int n = spelling.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(spelling.charAt(i))) i++;
            if (i == n) break;
            while (i < n && spelling.charAt(i) != '"') {
                if ("LuU8".indexOf(spelling.charAt(i)) < 0) return Optional.empty();
                i++;
            }
            if (i == n) return Optional.empty();
            i++; // opening quote
            while (i < n && spelling.charAt(i) != '"') {
                char c = spelling.charAt(i++);
                long unit;
                boolean ucn = false;
                if (c != '\\') {
                    unit = c;
                    if (narrow && c > 0x7f) {
                        bytes.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
                        continue;
                    }
                } else {
                    if (i == n) return Optional.empty();
                    char e = spelling.charAt(i++);
                    switch (e) {
                        case 'a' -> unit = 7;
                        case 'b' -> unit = 8;
                        case 'f' -> unit = 12;
                        case 'n' -> unit = 10;
                        case 'r' -> unit = 13;
                        case 't' -> unit = 9;
                        case 'v' -> unit = 11;
                        case '\\', '\'', '"', '?' -> unit = e;
                        case 'x' -> {
                            int start = i;
                            while (i < n && Character.digit(spelling.charAt(i), 16) >= 0) i++;
                            if (start == i) return Optional.empty();
                            unit = Long.parseLong(spelling.substring(start, i), 16);
                        }
                        case 'u', 'U' -> {
                            int len = e == 'u' ? 4 : 8;
                            if (i + len > n) return Optional.empty();
                            unit = Long.parseLong(spelling.substring(i, i + len), 16);
                            i += len;
                            ucn = true;
                        }
                        default -> {
                            if (e < '0' || e > '7') return Optional.empty();
                            int start = i - 1;
                            while (i < n && i - start < 3 && spelling.charAt(i) >= '0' && spelling.charAt(i) <= '7') i++;
                            unit = Long.parseLong(spelling.substring(start, i), 8);
                        }
                    }
                }
                if (narrow && ucn) {
                    bytes.writeBytes(Character.toString((int) unit).getBytes(StandardCharsets.UTF_8));
                } else if (narrow) {
                    bytes.write((int) unit);
                } else if (unit <= 0xFFFF) {
                    wide.append((char) unit);
                } else if (unit <= Character.MAX_CODE_POINT) {
                    wide.appendCodePoint((int) unit);
                } else {
                    return Optional.empty();
                }
            }
            if (i == n) return Optional.empty();
            i++; // closing quote
        }
        return Optional.of(narrow ? bytes.toString(StandardCharsets.UTF_8) : wide.toString());
    }
}
