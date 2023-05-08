package com.gestion.adhesion.repository;

import com.gestion.adhesion.models.Document;
import com.gestion.adhesion.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

}
