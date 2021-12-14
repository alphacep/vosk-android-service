package org.vosk.demo.utils;

import static org.vosk.demo.api.Download.CLEAR;
import static org.vosk.demo.api.Download.COMPLETE;

import org.vosk.demo.api.Download;
import org.vosk.demo.ui.model_list.ModelListActivity;

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

public class ZipHelper {

    public static void unzipFIle(File zipFilePath, File unzipAtLocation) {

        unzipAtLocation.mkdir();

        try (ZipFile zipfile = new ZipFile(zipFilePath)) {
            for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                unzipEntry(zipfile, entry, unzipAtLocation);
            }
            EventBus.getInstance().postDownloadStatus(new Download(COMPLETE));
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
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        String message = "unzipEntry(" + entry + ")[" + entry.getSize() + "] ";

        InputStream zin = zipfile.getInputStream(entry);

        try (BufferedInputStream input = new BufferedInputStream(zin);
             BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile));) {
            copy(input, output);
        } catch (IOException e) {
            throw new IOException(message, e);
        }
    }

    public static void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        boolean folderCreated = dir.mkdir();
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte data[] = new byte[10240];
        int count;

        int total = 0;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
            total += count;
        }
        output.flush();
    }
}
