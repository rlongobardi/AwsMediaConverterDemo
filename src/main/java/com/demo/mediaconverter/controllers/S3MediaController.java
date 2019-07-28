package com.demo.mediaconverter.controllers;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.demo.mediaconverter.s3.S3Handler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/mediaconvert/assets")
public class S3MediaController {

    private final S3Handler s3Handler;

    @Autowired
    public S3MediaController(S3Handler s3Handler) {
        this.s3Handler = s3Handler;
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public List<PutObjectResult> uploadAssets(@RequestParam("file") MultipartFile[] multipartFiles) {
        return s3Handler.upload(multipartFiles);
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public ResponseEntity<byte[]> downloadAssets(@RequestParam("fileName") String fileName) throws IOException {
        return s3Handler.download(fileName);
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<String> listAssets() throws IOException {
        return s3Handler.listAssetsDestination();
    }
}
