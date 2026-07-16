package project2.example.proj.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class DocumentRecord {

    @Id
    private String id;

    private String originalFileName;
    private String filePath;
    private String documentType;
    private String vendor;
    private Double totalAmount;
    private String currency;
    private LocalDate documentDate;

    @Column(length = 2000)
    private String summary;

    private LocalDateTime uploadedAt;
    private String status;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineItem> lineItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
