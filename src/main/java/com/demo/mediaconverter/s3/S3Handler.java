package com.demo.mediaconverter.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class S3Handler {


    private static final String MP_4 = ".mp4";

    private final AmazonS3 amazonS3;

    @Value("${mediaconvert.s3.bucket.origin}")
    private String bucketOrigin;

    @Value("${mediaconvert.s3.bucket}")
    private String bucketRoot;

    @Value("${mediaconvert.s3.transcoded-assets.folder}")
    private String mediaTranscoded;

    @Autowired
    public S3Handler(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    private static List<String> getListFiles(ListObjectsRequest listObjectsRequest, AmazonS3 amazonS3) {
        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);
        List<String> keys = new ArrayList<>();

        ObjectListing objects = amazonS3.listObjects(listObjectsRequest);
        List<S3ObjectSummary> summaries = objects.getObjectSummaries();

        while (summaries.size() > 0) {
            summaries.stream().filter(item -> !item.getKey().endsWith("/")).map(S3ObjectSummary::getKey).forEach(keys::add);
            summaries = amazonS3.listNextBatchOfObjects(objects).getObjectSummaries();
        }

        return keys;
    }

    public List<PutObjectResult> upload(MultipartFile[] multipartFiles) {
        List<PutObjectResult> putObjectResults = new ArrayList<>();
        ObjectMetadata metadata = new ObjectMetadata();

        Arrays.stream(multipartFiles)
                .filter(multipartFile -> !StringUtils.isEmpty(multipartFile.getOriginalFilename()))
                .forEach(multipartFile -> {
                    try {
                        metadata.setContentLength(multipartFile.getSize());
                        putObjectResults.add(upload(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), metadata));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        return putObjectResults;
    }

    public ResponseEntity<byte[]> download(String fileKey) throws IOException {

        final String keyBucket = String.format("%s/%s%s", mediaTranscoded, fileKey, MP_4);

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketRoot, keyBucket);

        S3Object s3Object = amazonS3.getObject(getObjectRequest);

        byte[] bytes = IOUtils.toByteArray(s3Object.getObjectContent());

        String fileName = URLEncoder.encode(keyBucket, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");

        return downloadResponse(bytes, fileName);
    }

    public List<String> listAssetsDestination() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketRoot)
                .withPrefix(mediaTranscoded + "/");
        return getListFiles(listObjectsRequest, amazonS3);
    }

    public List<String> listAssetsOrigin() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketOrigin).withPrefix("/");
        return getListFiles(listObjectsRequest, amazonS3);
    }

    private ResponseEntity<byte[]> downloadResponse(byte[] bytes, String fileName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentLength(bytes.length);
        httpHeaders.setContentDispositionFormData("attachment", fileName);

        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);
    }

    private PutObjectResult upload(InputStream fileStream, String uploadKey, ObjectMetadata metadata) throws FileNotFoundException {

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketOrigin, uploadKey, fileStream, metadata);

        PutObjectResult putObjectResult = amazonS3.putObject(putObjectRequest);

        IOUtils.closeQuietly(fileStream);

        return putObjectResult;

    }

}
