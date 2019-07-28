package com.demo.mediaconverter.service;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.mediaconvert.AWSMediaConvert;
import com.amazonaws.services.mediaconvert.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@EnableConfigurationProperties
@Service
public class MediaConvertService {

    private static final String REQUEST_HD_JSON = "classpath:template/mediaconvert_request_HD.json";
    private static final String MEDIACONVERT_REQUEST_JSON = "classpath:template/mediaconvert_request.json";
    private static final String VIDEO_QUEUE_TRANSCODER = "arn:aws:mediaconvert:eu-west-1:490871468036:queues/TestVideoQueueTranscoder";
    private static final String MEDIA_CONVERT_DEMO = "arn:aws:iam::490871468036:role/MediaConvert-Demo";

    private final AWSMediaConvert awsMediaConvert;

    private final ResourceLoader resourceLoader;

    @Value("${mediaconvert.s3.bucket.origin}")
    private String bucketFolderUpload;

    @Value("${mediaconvert.s3.bucket}")
    private String bucketRoot;

    @Value("${mediaconvert.s3.transcoded-assets.folder}")
    private String mediaTranscoded;

    @Value("${mediaconvert.url}")
    private String mediaConvertUrl;

    @Autowired
    public MediaConvertService(ResourceLoader resourceLoader, AWSMediaConvert awsMediaConvert) {
        this.resourceLoader = resourceLoader;
        this.awsMediaConvert = awsMediaConvert;
    }

    public String sendRequestToMediaConverter(String fileName, String quality) throws Exception {

        JobSettings jbSettings = editingTemplateRequest(fileName, quality);
        CreateJobRequest req = new CreateJobRequest();

        req.setQueue(VIDEO_QUEUE_TRANSCODER);
        req.setRole(MEDIA_CONVERT_DEMO);
        final JobSettings settings = getJobSettings(jbSettings, fileName);
        req.setSettings(settings);
        //req.setSettings(jobTemplateRequest.);
        CreateJobResult response = awsMediaConvert.createJob(req);
        ResponseMetadata responseStatus = response.getSdkResponseMetadata();
        return responseStatus.getRequestId();
    }

    private JobSettings getJobSettings(JobSettings jbSettings, String fileName) {
        final JobSettings settings = new JobSettings();
        List<Input> inp = new ArrayList<>();
        jbSettings.getInputs().forEach(ji -> {
            Input i = new Input();
            AudioSelector audioSelector = new AudioSelector()
                    .withOffset(0)
                    .withDefaultSelection("DEFAULT")
                    .withLanguageCode("ENM")
                    .withSelectorType("LANGUAGE_CODE")
                    .withProgramSelection(1);

            Map<String, AudioSelector> asg = new HashMap<>();
            asg.put("Audio Selector 1", audioSelector);
            i.setTimecodeSource(ji.getTimecodeSource());
            i.setFileInput(String.format("s3://%s/%s", bucketFolderUpload, fileName));
            i.setAudioSelectors(asg);
            inp.add(i);
        });
        settings.setInputs(inp);
        settings.setAdAvailOffset(jbSettings.getAdAvailOffset());
        final List<OutputGroup> outputGroups = jbSettings.getOutputGroups();
        final String destination = String.format("s3://%s/%s/", bucketRoot, mediaTranscoded);
        outputGroups.get(0).getOutputGroupSettings().getFileGroupSettings().setDestination(destination);
        settings.setOutputGroups(outputGroups);
        return settings;
    }

    private JobSettings editingTemplateRequest(String fileName, String quality) throws IOException {
        FileReader reader = null;
        if (quality.toUpperCase().equals("HD")) {
            reader = new FileReader(resourceLoader.getResource(REQUEST_HD_JSON)
                    .getFile());
        } else {
            reader = new FileReader(resourceLoader.getResource(MEDIACONVERT_REQUEST_JSON)
                    .getFile());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(reader);
        return objectMapper.convertValue(rootNode, JobSettings.class);
    }

}
