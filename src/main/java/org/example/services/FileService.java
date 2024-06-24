
package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileService {

    public String fileDirectory = "";

    public FileService(@Value("${file.directory}") String fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    public List<String> getFileNames(int offset, int limit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

        try (
            Stream<String> files = Files.list(Paths.get(fileDirectory))
                .map(path -> path.getFileName().toString())
                //.filter(this::isValidFileName)
                .sorted(Comparator.comparing(path -> {
                    String datePart = path.split("--")[0];
                    LocalDateTime dateTime = LocalDateTime.parse(datePart, formatter);
                    return dateTime;
                }, Comparator.reverseOrder()))
                .skip(offset * limit)
                .limit(limit)) {
            return files.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading files", e);
        }
    }
    public List<String> getPrivateFileNames(int offset, int limit, String user_id) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        try (
                Stream<String> files = Files.list(Paths.get(fileDirectory))
                        .map(path -> path.getFileName().toString())
                        .filter(path -> {
                            String[] parts = path.split("--");
                            return parts.length > 1 && parts[1].equals(user_id);
                        })
                        .sorted(Comparator.comparing(path -> {
                            String datePart = path.split("--")[0];
                            LocalDateTime dateTime = LocalDateTime.parse(datePart, formatter);
                            return dateTime;
                        }, Comparator.reverseOrder()))
                        .skip(offset * limit)
                        .limit(limit)) {
            return files.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error reading files", e);
        }
    }

    private boolean isValidFileName(String fileName) {
        return fileName.matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}--[a-f0-9-]+--[a-f0-9-]+--[^%]+");
    }

    /*public String compressFilesToGzip(List<String> fileNames) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            for (String fileName : fileNames) {
                try (FileInputStream fileInputStream = new FileInputStream(Paths.get(fileDirectory, fileName).toFile())) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fileInputStream.read(buffer)) > 0) {
                        gzipOutputStream.write(buffer, 0, len);
                    }
                }
            }
            gzipOutputStream.finish();
            return byteArrayOutputStream.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error compressing files", e);
        }
    }*/
}
