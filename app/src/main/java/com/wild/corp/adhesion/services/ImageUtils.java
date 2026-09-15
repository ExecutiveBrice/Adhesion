package com.wild.corp.adhesion.services;

import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ImageUtils {

    private ImageUtils() {
    }

    public static byte[] compressImage(byte[] data) {
        Objects.requireNonNull(data, "Les données à compresser sont obligatoires");
        Deflater deflater = new Deflater();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        try {
            deflater.setLevel(Deflater.BEST_COMPRESSION);
            deflater.setInput(data);
            deflater.finish();

            byte[] buffer = new byte[4 * 1024];
            while (!deflater.finished()) {
                int size = deflater.deflate(buffer);
                outputStream.write(buffer, 0, size);
            }
            return outputStream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    public static byte[] decompressImage(byte[] data) {
        Objects.requireNonNull(data, "Les données à décompresser sont obligatoires");
        Inflater inflater = new Inflater();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        try {
            inflater.setInput(data);
            byte[] buffer = new byte[4 * 1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new IllegalArgumentException("Les données compressées sont incomplètes ou invalides");
                }
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } catch (DataFormatException exception) {
            throw new IllegalArgumentException("Les données compressées sont invalides", exception);
        } finally {
            inflater.end();
        }
    }
}
