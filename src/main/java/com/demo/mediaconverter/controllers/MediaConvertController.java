package com.demo.mediaconverter.controllers;

import com.demo.mediaconverter.service.MediaConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/mediaconvert")
public class MediaConvertController {

    private final MediaConvertService mediaConvertService;

    @Autowired
    public MediaConvertController(MediaConvertService mediaConvertService) {
        this.mediaConvertService = mediaConvertService;
    }


    @RequestMapping(value = "/getJobs", method = RequestMethod.GET)
    public String getJobList() {
        return "jobs";
    }

    @RequestMapping(value = "/startTranscodeJob", method = RequestMethod.GET)
    public ResponseEntity mediaTranscodeJob(@RequestParam(name = "fileName") String fileName,
                                            @RequestParam(name = "quality", defaultValue = "STANDARD") String quality) throws Exception {

        final String responseId = mediaConvertService.sendRequestToMediaConverter(fileName, quality);
        return new ResponseEntity<>(responseId, HttpStatus.OK);

    }

}
