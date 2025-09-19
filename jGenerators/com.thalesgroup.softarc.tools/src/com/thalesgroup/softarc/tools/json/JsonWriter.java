/* Copyright (c) 2025 THALES -- All rights reserved */

package com.thalesgroup.softarc.tools.json;

import java.io.IOException;

public class JsonWriter {

    final public Appendable out;
    private boolean first;
    public int depth = 0;

    public JsonWriter(Appendable output) {
        out = output;
    }

    public void putString(String string) throws IOException {
        if (string == null) {
            out.append("null");
        } else {
            out.append('\"');
            quoteString(string);
            out.append('\"');
        }
    }

    public void quoteString(String string) throws IOException {
        int len = string.length();
        for (int i = 0; i < len; i += 1) {
            char c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                out.append('\\');
                out.append(c);
                break;
            case '\b':
                out.append("\\b");
                break;
            case '\t':
                out.append("\\t");
                break;
            case '\n':
                out.append("\\n");
                break;
            case '\r':
                out.append("\\r");
                break;
            default:
                if (c < ' ') {
                    out.append("\\u");
                    String t = Integer.toHexString(c);
                    for (int j = 4 - t.length(); j > 0; j--) {
                        out.append('0');
                    }
                    out.append(t);
                } else {
                    out.append(c);
                }
            }
        }
    }

    public void newLine() throws IOException {
        out.append('\n');
        for (int i = depth; i > 0; i--)
            out.append(' ');
    }

    public void beginList(int size) throws IOException {
        depth++;
        out.append('[');
        first = true;
    }

    public void beforeListElement() throws IOException {
        if (!first) {
            out.append(',');
        }
        first = false;
        newLine();
    }

    public void endList() throws IOException {
        depth--;
        out.append(']');
    }

    public void beginObject() throws IOException {
        depth++;
        out.append('{');
        first = true;
    }

    public void beforeField(String name) throws IOException {
        if (!first) {
            out.append(',');
            newLine();
        }
        first = false;
        out.append('"');
        out.append(name);
        out.append('"');
        out.append(':');
    }

    public void endObject() throws IOException {
        out.append('}');
        depth--;
    }

}
