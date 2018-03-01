package org.langwiki.brime.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class FileHelper {
    public static String loadFile(File file, String defResult) {
        try (
            InputStream is = new FileInputStream(file);
        ) {
            return read(is);
        } catch (IOException e) {
            e.printStackTrace();
            return defResult;
        }
    }

    public static String read(InputStream is) {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

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

    public static void writeFile(File file, String text) {
        try (
                OutputStream os = new FileOutputStream(file);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        ) {
            bw.write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
