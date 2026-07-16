## Task 1: Add Dependencies and Configure application.properties

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Produces: working app startup with H2 console at `http://localhost:8080/h2-console`

- [ ] **Step 1: Add dependencies to pom.xml**

Open `pom.xml`. Inside `<dependencies>`, add after the existing Lombok entry:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.1</version>
</dependency>
```

- [ ] **Step 2: Replace application.properties**

Replace the entire contents of `src/main/resources/application.properties` with:

```properties
spring.application.name=proj

# H2 in-memory database
spring.datasource.url=jdbc:h2:mem:docdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload.dir=uploads/

# Gemini API key — set as env var GEMINI_API_KEY before running
gemini.api.key=${GEMINI_API_KEY}
```

- [ ] **Step 3: Verify the app starts**

Run:
```
./mvnw spring-boot:run -DGEMINI_API_KEY=placeholder
```
Expected: App starts on port 8080, no errors. H2 console available at `http://localhost:8080/h2-console`. Stop the app with Ctrl+C.

---

