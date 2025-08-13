package org.flatizy.flatizy.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class FileService {

    public void processFiles(String path, Consumer<File> action) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (File file : Objects.requireNonNull(listOfFiles)) {
            if (file.isFile()) {
                action.accept(file);
            }
        }
    }

    public void renameFile(File file) {
        String fileText = parsePdfFile(file);
        int building = extractNumberAfter(fileText, "корпус");
        int apartment = extractNumberAfter(fileText, "кв.");
        if (building > 0 && apartment >= 0) {

            String newFileName = String.format("%d%03d", building * 100, apartment);
            File renamed = new File(file.getParent(), newFileName + getFileExtension(file.getName()));

            boolean success = file.renameTo(renamed);
            if (success) {
                System.out.println("File renamed: " + renamed.getName());
            } else {
                System.out.println("Error during rename file");
            }
        } else {
            System.out.println("Cann't get apartment or building from text");
        }
    }

    String createTelegramName(int buildNumber, int apartmentNumber) {
        return buildNumber == 5 ? String.format("%d%03d", buildNumber * 100, apartmentNumber) : String.format("%03d", apartmentNumber);
    }

    String parsePdfFile(File file) {
        PDDocument document;
        try {
            document = Loader.loadPDF(new RandomAccessReadBufferedFile(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (document.isEncrypted()) {
            try {
                throw new Exception("Document is encrypted.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        PDFTextStripper pdfStripper = new PDFTextStripper();
        try {
            return pdfStripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Integer extractNumberAfter(String text, String key) {
        int index = text.toLowerCase().indexOf(key.toLowerCase());
        if (index == -1) return -1;

        String afterKey = text.substring(index + key.length()).trim();

        StringBuilder number = new StringBuilder();
        for (char c : afterKey.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (!number.isEmpty()) {
                break;
            }
        }
        return !number.isEmpty() ? Integer.parseInt(number.toString()) : -1;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }
        return "";
    }


}
