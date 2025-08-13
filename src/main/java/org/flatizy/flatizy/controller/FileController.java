package org.flatizy.flatizy.controller;

import org.flatizy.flatizy.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/files")
@RestController()
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    private static final String PATH_FILE = "src/main/resources/files/";

    @GetMapping("/rename")
    public ResponseEntity<Void> renameFiles() {
        fileService.processFiles(PATH_FILE, fileService::renameFile);
        return ResponseEntity.ok().build();
    }
}
