package com.codingshuttle.distributed_lovable.workspace_service.controller;

import com.codingshuttle.distributed_lovable.common_lib.dto.FileTreeDto;
import com.codingshuttle.distributed_lovable.workspace_service.dto.project.FileContentResponse;
import com.codingshuttle.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class FileController {

    ProjectFileService projectFileService;

    @GetMapping
    public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId){
        return ResponseEntity.ok(projectFileService.getFileTree(projectId));
    }

    @GetMapping("/content") // if path is like /src/hooks/AppHook.jsx, I will get all path after/
    public ResponseEntity<String> getFile(
            @PathVariable Long projectId,
            @RequestParam String path){
        return ResponseEntity.ok(projectFileService.getFileContent(projectId,path));
    }
}
