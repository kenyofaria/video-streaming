package com.streaming.example.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VideoStreamingService {

    private static final String VIDEO_DIRECTORY = "videos/"; // Place videos in src/main/resources/videos/
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(?<start>\\d+)-(?<end>\\d*)");

    /**
     * Stream video with HTTP Range support for seeking
     */
    public ResponseEntity<Resource> streamVideo(String fileName, String rangeHeader) {
        try {
            Path videoPath = getVideoPath(fileName);
            if (!Files.exists(videoPath)) {
                return ResponseEntity.notFound().build();
            }

            long fileSize = Files.size(videoPath);

            if (rangeHeader == null || rangeHeader.isEmpty()) {
                // No range header - return full file
                return createFullFileResponse(videoPath, fileSize);
            } else {
                // Range request - return partial content. This path should be the most used
                return createRangeResponse(videoPath, fileSize, rangeHeader);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create response for full file (no range)
     */
    private ResponseEntity<Resource> createFullFileResponse(Path videoPath, long fileSize) throws IOException {
        Resource video = new UrlResource(videoPath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .contentLength(fileSize)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(video);
    }

    /**
     * Create response for range request (partial content)
     */
    private ResponseEntity<Resource> createRangeResponse(Path videoPath, long fileSize, String rangeHeader) throws IOException {
        Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);

        if (!matcher.matches()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long start = Long.parseLong(matcher.group("start"));
        String endGroup = matcher.group("end");
        long end = endGroup.isEmpty() ? Math.min(start + CHUNK_SIZE - 1, fileSize - 1) : Long.parseLong(endGroup);

        // Validate range
        if (start >= fileSize || end >= fileSize || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long contentLength = end - start + 1;

        // Create custom resource for range
        Resource rangeResource = new ByteRangeResource(videoPath, start, contentLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .contentLength(contentLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, String.format("bytes %d-%d/%d", start, end, fileSize))
                .body(rangeResource);
    }

    /**
     * Get video metadata
     */
    public ResponseEntity<?> getVideoInfo(String fileName) {
        try {
            Path videoPath = getVideoPath(fileName);
            if (!Files.exists(videoPath)) {
                return ResponseEntity.notFound().build();
            }

            long fileSize = Files.size(videoPath);

            Map<String, Object> info = new HashMap<>();
            info.put("fileName", fileName);
            info.put("fileSize", fileSize);
            info.put("fileSizeMB", fileSize / (1024.0 * 1024.0));
            info.put("contentType", "video/mp4");
            info.put("supportsRangeRequests", true);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
    /**
     * List available videos
     */
    public ResponseEntity<?> listAvailableVideos() {
        try {
            Path videosDir = Paths.get(getClass().getClassLoader().getResource(VIDEO_DIRECTORY).toURI());

            List<String> videos = new ArrayList<>();
            Files.list(videosDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                    .forEach(path -> videos.add(path.getFileName().toString()));

            return ResponseEntity.ok(videos);

        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    private Path getVideoPath(String fileName) throws Exception {
        URL resource = getClass().getClassLoader().getResource(VIDEO_DIRECTORY + fileName);
        if (resource == null) {
            throw new FileNotFoundException("File not found");
        }
        return Paths.get(resource.toURI());
    }
}