package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/files")
@RestController()
public class FileController {

    FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    private static final String PATH_FILE = "C:/Users/Danya/Documents/спам/";

    @GetMapping("/get")
    public ResponseEntity<Void> getFiles() {
        fileService.getFiles(PATH_FILE);
        return ResponseEntity.ok().build();
    }
}
