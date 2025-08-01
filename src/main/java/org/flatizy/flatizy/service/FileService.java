package org.flatizy.flatizy.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Service
public class FileService {


    public void getFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (File file : Objects.requireNonNull(listOfFiles)) {
            if (file.isFile()) {
                renameFile(file);
            }
        }
    }

    private void renameFile(File file) {
        String fileText = parseFile(file);
        int corpus = extractNumberAfter(fileText, "корпус");
        int kv = extractNumberAfter(fileText, "кв.");
        if (corpus > 0 && kv >= 0) {

            String newFileName = String.format("%d%03d", corpus * 100, kv);
            File renamed = new File(file.getParent(), newFileName + getFileExtension(file.getName()));

            boolean success = file.renameTo(renamed);
            if (success) {
                System.out.println("Файл переименован: " + renamed.getName());
            } else {
                System.out.println("Ошибка при переименовании файла.");
            }
        } else {
            System.out.println("Не удалось извлечь корпус или квартиру из текста.");
        }
    }

    private String findApartmentNumber(File file) {
        String fileText = parseFile(file);
        int accNumber = extractNumberAfter(fileText, "рахунку");
        return null;
    }

    private String parseFile(File file) {
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
            System.out.println(pdfStripper.getText(document));
            return pdfStripper.getText(document);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int extractNumberAfter(String text, String key) {
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
