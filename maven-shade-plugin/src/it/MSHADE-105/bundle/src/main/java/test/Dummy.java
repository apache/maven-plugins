package test;

import internal.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class Dummy {
    public static void dump(InputStream in) throws IOException {
        IOUtils.copy(in, System.out);
    }
}
