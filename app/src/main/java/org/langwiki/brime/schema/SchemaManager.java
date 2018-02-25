package org.pocketworkstation.pckeyboard.schema;

public class SchemaManager {
    private static SchemaManager sInstance;
    private Resources resources;

    public static SchemaManager getInstance() {
        if (sInstance != null)
            return sInstance;

        synchronized (SchemaManager.class) {
            sInstance = new SchemaManager();
            return sInstance;
        }
    }

    public SchemaManager() {
    }

    //load file from apps res/raw folder or Assets folder
    public String loadFile(String fileName, boolean loadFromRawFolder) throws IOException
    {
        //Create a InputStream to read the file into
        InputStream iS;

        if (loadFromRawFolder)
        {
            //get the resource id from the file name
            int rID = resources.getIdentifier("fortyonepost.com.lfas:raw/" + fileName, null, null);
            //get the file as a stream
            iS = resources.openRawResource(rID);
        } else {
            //get the file as a stream
            iS = resources.getAssets().open(fileName);
        }

        //create a buffer that has the same size as the InputStream
        byte[] buffer = new byte[iS.available()];
        //read the text file as a stream, into the buffer
        iS.read(buffer);
        //create a output stream to write the buffer into
        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        //write this buffer to the output stream
        oS.write(buffer);
        //Close the Input and Output streams
        oS.close();
        iS.close();

        //return the output stream as a String
        return oS.toString();
    }
}