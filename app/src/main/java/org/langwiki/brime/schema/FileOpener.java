package org.langwiki.brime.schema;

import java.io.IOException;
import java.io.InputStream;

public interface FileOpener {
    InputStream open(String path) throws IOException;
}
