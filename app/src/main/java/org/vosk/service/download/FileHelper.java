package org.vosk.service.download;

import static org.vosk.service.download.Download.CLEAR;
import static org.vosk.service.download.Download.COMPLETE;

import android.content.Context;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.IOUtils;

import org.vosk.service.ui.selector.ModelListActivity;
import org.vosk.service.utils.Tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileHelper {

    public static void unzipFIle(Context context,File zipFilePath, File unzipAtLocation) {

        //noinspection ResultOfMethodCallIgnored
        unzipAtLocation.mkdir();

        try (ZipFile zipfile = new ZipFile(zipFilePath)) {
            for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                unzipEntry(zipfile, entry, unzipAtLocation);
            }
            EventBus.getInstance().postDownloadStatus(new Download(COMPLETE));
            FileHelper.deleteFileOrDirectory(new File(Tools.getModelFileRootPath(context), unzipAtLocation.getName() + ".zip"));
        } catch (IOException e) {
            ModelListActivity.progress = CLEAR;
            EventBus.getInstance().postErrorStatus(Error.CONNECTION);
            e.printStackTrace();
        }
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }

        File outputFile = new File(outputDir, entry.getName());
        if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        String message = "unzipEntry(" + entry + ")[" + entry.getSize() + "] ";

        InputStream zin = zipfile.getInputStream(entry);

        try (BufferedInputStream input = new BufferedInputStream(zin);
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            copy(input, output);
        } catch (IOException e) {
            throw new IOException(message, e);
        }
    }

    public static void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        dir.mkdir();
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] data = new byte[10240];
        int count;

        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }
        output.flush();
    }

    public static void writeFile(InputStream inputStream, File file) {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            EventBus.getInstance().postErrorStatus(Error.WRITE_STORAGE);
        }
    }

    public static void deleteFileOrDirectory(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteFileOrDirectory(child);
            }
        }

        fileOrDirectory.delete();
    }
}
