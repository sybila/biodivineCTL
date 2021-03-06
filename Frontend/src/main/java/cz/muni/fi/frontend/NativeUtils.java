package cz.muni.fi.frontend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

/**
 * Simple library class for working with JNI (Java Native Interface)
 *
 * @see "http://frommyplayground.com/how-to-load-native-jni-library-from-jar"
 *
 * @author Adam Heirnich &lt;adam@adamh.cz&gt;, http://www.adamh.cz
 */
public class NativeUtils {


    public static void loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            System.err.println(name+" module loaded from include path.");
        } catch (UnsatisfiedLinkError e) {
            try {
                switch (OsCheck.getOperatingSystemType()) {
                    case MacOS:
                        NativeUtils.loadLibraryFromJar("/lib"+name+".jnilib");
                        break;
                    case Linux:
                        NativeUtils.loadLibraryFromJar("/lib"+name+".so");
                        break;
                    default:
                        System.err.println("Unsupported operating system for module: "+name);
                        break;
                }
                System.err.println(name+" module loaded from jar file.");
            } catch (Exception e1) {
                System.err.println("Unable to load module: "+name+", problem: "+e1.toString());
            }
        }
    }

    /**
     * Private constructor - this class will never be instanced
     */
    private NativeUtils() {
    }

    /**
     * Loads library from current JAR archive
     *
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     *
     * @param path The filename inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext
     * @throws IOException If temporary file creation or read/write operation fails
     * @throws IllegalArgumentException If source file (param path) does not exist
     * @throws IllegalArgumentException If the path is not absolute or if the filename is shorter than three characters (restriction of {@see File#createTempFile(java.lang.String, java.lang.String)}).
     */
    public static void loadLibraryFromJar(@NotNull String path) throws IOException {

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }

        // Obtain filename from path
        @NotNull String[] parts = path.split("/");
        @Nullable String filename = (parts.length > 1) ? parts[parts.length - 1] : null;

        // Split filename to prexif and suffix (extension)
        String prefix = "";
        @Nullable String suffix = null;
        if (filename != null) {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "."+parts[parts.length - 1] : null; // Thanks, davs! :-)
        }

        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }

        // Prepare temporary file
        @NotNull File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();

        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }

        // Prepare buffer for data copying
        @NotNull byte[] buffer = new byte[1024];
        int readBytes;

        // Open and check input stream
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }

        // Open output stream and copy data between source file in JAR and the temporary file
        try (@NotNull OutputStream os = new FileOutputStream(temp)) {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an exception
            is.close();
        }

        // Finally, load the library
        System.load(temp.getAbsolutePath());
    }
}
