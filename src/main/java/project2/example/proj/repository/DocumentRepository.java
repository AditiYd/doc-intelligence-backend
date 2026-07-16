package project2.example.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project2.example.proj.model.DocumentRecord;

public interface DocumentRepository extends JpaRepository<DocumentRecord, String> {
}
