package org.example.services;

import org.example.repository.ImageRepository;
import org.example.model.Image;

import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    public Image saveImage(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getOriginalFilename());
        image.setData(file.getBytes());
        return imageRepository.save(image);
    }
    public Image saveImage(MultipartFile file, String filename) throws IOException {
        Image image = new Image();
        image.setName(filename);
        image.setData(file.getBytes());
        return imageRepository.save(image);
    }

    public Optional<Image> getImage(Long id) {
        return imageRepository.findById(id);
    }

    public Optional<Image> getImage(String filename) {

        return imageRepository.findByName(filename);
    }

    public Page<String> getList(int offset, int count) {
        PageRequest page = PageRequest.of(offset, count);
        return imageRepository.findAllImageNames(page);
    }
    public Page<String> getPrivateList(String user_id, int offset, int count) {
        PageRequest page = PageRequest.of(offset, count);
        return imageRepository.findByRegularName(user_id, page);
    }

    public void removeImage(String filename) {
        imageRepository.deleteByName(filename);
    }
}
