package org.langwiki.brime.utils;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileHelper {
    public static boolean copyTo(InputStream fis, String filePath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(filePath));
            byte[] buffer = new byte[1024];
            int noOfBytes;

            System.out.println("Copying file using streams");

            // read bytes from source file and write to destination file
            while ((noOfBytes = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, noOfBytes);
            }

            return true;
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
            return false;
        }
        catch (IOException ioe) {
            System.out.println("Exception while copying file " + ioe);
            return false;
        }
        finally {
            // close the streams using close method
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }
            catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
    }
}
