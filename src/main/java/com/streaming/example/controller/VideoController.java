package com.streaming.example.controller;

import com.streaming.example.service.VideoStreamingService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "*")
public class VideoController {

    private final VideoStreamingService videoStreamingService;

    public VideoController(VideoStreamingService videoStreamingService) {
        this.videoStreamingService = videoStreamingService;
    }

    /**
     * Stream video with range request support
     * This endpoint handles HTTP Range requests for video seeking
     */
    @GetMapping("/stream/{fileName}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {

        return videoStreamingService.streamVideo(fileName, rangeHeader);
    }

    /**
     * Get video metadata (duration, size, etc.)
     */
    @GetMapping("/info/{fileName}")
    public ResponseEntity<?> getVideoInfo(@PathVariable String fileName) {
        return videoStreamingService.getVideoInfo(fileName);
    }

    /**
     * List available videos
     */
    @GetMapping("/list")
    public ResponseEntity<?> listVideos() {
        return videoStreamingService.listAvailableVideos();
    }
}
