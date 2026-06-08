package com.daku.diary.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 업로드 폴더 절대경로 확보 + 없으면 생성
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // 원본 확장자 추출
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        // 충돌 방지용 랜덤 파일명
        String filename = UUID.randomUUID().toString() + ext;
        Path target = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), target);

        // DB엔 이 경로(URL)만 저장됨
        Map<String, String> result = new HashMap<>();
        result.put("url", "/uploads/" + filename);
        return result;
    }
}