package org.example.repository;

import org.example.model.Image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("SELECT i.name FROM Image i ORDER BY i.id DESC")
    Page<String> findAllImageNames(Pageable pageable);

    @Query("SELECT i FROM Image i WHERE i.name = :name")
    Optional<Image> findByName(@Param("name") String filename);

    @Transactional
    @Modifying
    @Query("DELETE FROM Image i WHERE i.name = :name")
    void deleteByName(@Param("name") String name);

    @Query("SELECT i.name FROM Image i WHERE i.name LIKE %:namePart% ORDER BY i.id DESC")
    Page<String> findByRegularName(@Param("namePart") String namePart, Pageable pageable);

}
