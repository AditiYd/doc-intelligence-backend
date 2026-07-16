package project2.example.proj.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentRecordTest {

    @Test
    void prePersist_setsUuidId() {
        DocumentRecord doc = new DocumentRecord();
        doc.prePersist();
        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getId()).hasSize(36); // UUID length
    }

    @Test
    void lineItems_defaultsToEmptyList() {
        DocumentRecord doc = new DocumentRecord();
        assertThat(doc.getLineItems()).isNotNull();
        assertThat(doc.getLineItems()).isEmpty();
    }
}
