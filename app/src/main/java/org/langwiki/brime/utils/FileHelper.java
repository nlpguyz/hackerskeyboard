package org.langwiki.brime.utils;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileHelper {
    public static boolean copyTo(InputStream is, String filePath) {
        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                FileOutputStream fos = new FileOutputStream(filePath);
                OutputStreamWriter osw = new OutputStreamWriter(fos);) {
            String line;
            while ((line = br.readLine()) != null) {
                osw.write(line);
                osw.write("\n");
            }
            osw.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
