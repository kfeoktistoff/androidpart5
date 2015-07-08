package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import retrofit.http.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Kirill Feoktistov on 08.07.15
 */
@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long, Video> videos = new HashMap<>();

    @Autowired
    private VideoFileManager videoFileManager;

    @RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.GET)
    public
    @ResponseBody
    Collection<Video> getVideoList() {
        return videos.values();
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_SVC_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    Video addVideo(@RequestBody Video v, HttpServletResponse resp) throws IOException {
        checkAndSetId(v);
        save(v);
        return v;
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    VideoStatus setVideoData(
            @PathVariable(VideoSvcApi.ID_PARAMETER) long id,
            @RequestPart(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData, HttpServletResponse response) throws IOException {

        if (videos.get(id) == null) {
            response.setStatus(404);
            return null;
        }

        Video v = Video.create().build();
        v.setId(id);
        v.setDataUrl(getDataUrl(id));

        videoFileManager.saveVideoData(v, videoData.getInputStream());
        return new VideoStatus(videoFileManager.hasVideoData(v) ? VideoStatus.VideoState.READY : VideoStatus.VideoState.PROCESSING);
    }

    @RequestMapping(value = VideoSvcApi.VIDEO_DATA_PATH, method = RequestMethod.GET)
    public void getData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, HttpServletResponse response) throws IOException {
        Video v = videos.get(id);

        if (v == null) {
            response.setStatus(404);
        } else {
            videoFileManager.copyVideoData(v, response.getOutputStream());
        }
    }

    public Video save(Video entity) {
        checkAndSetId(entity);
        entity.setDataUrl(getDataUrl(entity.getId()));
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if (entity.getId() == 0) {
            entity.setId(currentId.incrementAndGet());
        }
    }

    private String getDataUrl(long videoId) {
        return getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return "http://" + request.getServerName() + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
    }
}
