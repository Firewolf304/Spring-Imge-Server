package org.example;

import org.example.model.Image;
import org.example.security.CustomUserDetails;
import org.example.services.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.time.format.DateTimeFormatter;


@RestController
public class Controller {
    //============== local
    Logger logger = LoggerFactory.getLogger(App.class);

    @Value("${file.directory}")
    String ImagesPath = "";

    @Value("${file.delete-access}")
    int DeleteAccess = 1;

    @Value("${file.save-like-file}")
    boolean saveLikeFile = false;
    //============== local

    @Autowired
    private CustomUserDetails userDetailsService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private FileService files;


    @Autowired
    public Controller(FileService fileService) {
        this.files = fileService;
    }




    @GetMapping("/images")
    public ResponseEntity<String> GetList(@CookieValue(name = "id", required = false) String sess_id, @RequestParam(name = "offset", required = false, defaultValue = "0") int offset, @RequestParam(name = "count", required = false, defaultValue = "0") int count) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        UUID user_id = new UUID(0,0);
        if(!sess_id.isEmpty()) {
            user_id = UUID.fromString(userDetailsService.getUserIDBySession(UUID.fromString(sess_id)).toString());
        }

        //responseHeaders.add("Content-Type","image/jpg");
        //responseHeaders.add("Content-Encoding","gzip");
        String reason = "";
        if(offset <= 0) {
            reason += "Offset='offset <= 0'; ";
        }
        if(count <= 0 || count >= 10) {
            reason += "Count='incorrect value'; ";
        }
        if(!reason.equals("")) {
            responseHeaders.add("Content-Edit-Reason", reason);
        }
        List<String> fileNames = new ArrayList<>();
        var obj = new JSONObject();
        obj.put("images", new JSONArray());
        obj.put("delete_access", new JSONArray());
        if(saveLikeFile) {
            fileNames = files.getFileNames(offset, count);
        } else {
            fileNames = imageService.getList(offset, count).toList();
        }
        for(var a : fileNames) {
            ((JSONArray)obj.get("images")).put(a);
            if(!sess_id.isEmpty()) {
                var data = a.split("--");
                var access = userDetailsService.getAccessByUserID(user_id);
                if(access <= DeleteAccess || Objects.equals(data[1], user_id.toString())) {
                    ((JSONArray)obj.get("delete_access")).put(a);
                }
            }
        }
        return ResponseEntity.ok().headers(responseHeaders).body(obj.toString());
    }

    @GetMapping("/privateimages")
    public ResponseEntity<String> GetPrivateList(@CookieValue(name = "id", required = false) String sess_id, @RequestParam(name = "user_id", required = false, defaultValue = "0") String user_id, @RequestParam(name = "offset", required = false, defaultValue = "0") int offset, @RequestParam(name = "count", required = false, defaultValue = "0") int count) throws IOException {
        HttpHeaders responseHeaders = new HttpHeaders();
        Boolean self = false;
        if(!sess_id.isEmpty()  ) {
            var sess_user_id = userDetailsService.getUserIDBySession(UUID.fromString(sess_id)).toString();
            if(sess_user_id.equals(user_id)) {
                self = true;
            }
        }

        //responseHeaders.add("Content-Type","image/jpg");
        //responseHeaders.add("Content-Encoding","gzip");
        String reason = "";
        if(offset <= 0) {
            reason += "Offset='offset <= 0'; ";
        }
        if(count <= 0 || count >= 10) {
            reason += "Count='incorrect value'; ";
        }
        if(!reason.equals("")) {
            responseHeaders.add("Content-Edit-Reason", reason);
        }
        File folder = new File(ImagesPath);
        List<String> fileNames = new ArrayList<>();
        if(saveLikeFile) {
            fileNames = files.getPrivateFileNames(offset, count, user_id);
        } else {
            fileNames = imageService.getPrivateList(user_id, offset, count).toList();
        }
        var obj = new JSONObject();
        obj.put("images", new JSONArray());
        obj.put("delete_access", new JSONArray());

        for(var a : fileNames) {
            ((JSONArray)obj.get("images")).put(a);
            if(!sess_id.isEmpty()) {
                var data = a.split("--");
                var access = userDetailsService.getAccessByUserID(UUID.fromString(user_id));
                if(access <= DeleteAccess || Objects.equals(data[1], user_id.toString())) {
                    ((JSONArray)obj.get("delete_access")).put(a);
                }
            }
        }
        return ResponseEntity.ok().headers(responseHeaders).body(obj.toString());
    }

    @RequestMapping(value = "/getfile/{file}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> downloadFile(@PathVariable("file") String fileName) {
        try {
            if(saveLikeFile) {
                Path filePath = Paths.get(ImagesPath).resolve(fileName).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("image/jpeg"))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                            .body(resource.getContentAsByteArray());
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }
            } else {
                Optional<Image> imageOptional = imageService.getImage(fileName);
                if (imageOptional.isPresent()) {
                    Image image = imageOptional.get();
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("image/jpeg"))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getName() + "\"")
                            .body(image.getData());
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/apis/upload")
    public ResponseEntity<String> handleFileUpload(@CookieValue("id") UUID id, @RequestParam("title") String title,
                                                   @RequestParam("image") MultipartFile file) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
            LocalDateTime now = LocalDateTime.now();
            String dateTime = now.format(formatter);

            var user_id = jdbcTemplate.queryForList("select * from sessions where id = ?", id).get(0);
            if(user_id.isEmpty()) {
                throw new Exception("Bad request");
            }
            var username = jdbcTemplate.queryForList("select * from users where id = ?", UUID.fromString(user_id.get("user_id").toString())).get(0);
            if(username.isEmpty()) {
                throw new Exception("Bad request name");
            }
            title = title.replace("--", "__").replace("%", "");
            String filename = dateTime + "--" + user_id.get("user_id") + "--" + username.get("username") + "--" + title + ".jpg";
            if(saveLikeFile) {
                byte[] bytes = file.getBytes();
                Path path = Paths.get(ImagesPath + filename);
                Files.write(path, bytes);
            } else {
                imageService.saveImage(file, filename);
            }
            return ResponseEntity.ok().body("Ok");
        } catch (Exception e) {
            logger.error("Error upload: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error upload: " + e.getMessage());
        }
    }
    @RequestMapping( value = "/apis/remove", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> handleFileRemove(@CookieValue("id") UUID sess_id, @RequestParam(name = "name", required = true) String name) {

        try {

            UUID user_id = UUID.fromString(userDetailsService.getUserIDBySession(sess_id).toString());
            var access = userDetailsService.getAccessByUserID(user_id);
            var data = name.split("--");
            if(access <= DeleteAccess || Objects.equals(data[1], user_id.toString())) {

                if(saveLikeFile) {
                    File myObj = new File(ImagesPath + name);
                    if (myObj.delete()) {
                        return ResponseEntity.ok().body("OK");
                    } else {
                        return ResponseEntity.badRequest().body("Error remove:");
                    }
                } else {
                    imageService.removeImage(name);
                    return ResponseEntity.ok().body("OK");
                }
            }

        } catch (Exception e) {
            logger.error("Error remove: " + e.getMessage());
        }
        return ResponseEntity.badRequest().body("Error remove");
    }
}
