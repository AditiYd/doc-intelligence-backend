# Task 3: Repository - IMPLEMENTATION BLOCKED

## Status: NEEDS_CONTEXT

Task 3 cannot be completed due to a **missing test dependency** in pom.xml established in Task 1.

## Problem Summary

The task brief (task-3-brief.md) requires implementing tests using the `@DataJpaTest` annotation:

```java
@DataJpaTest
class DocumentRepositoryTest {
    @Autowired
    private DocumentRepository documentRepository;
    // ...
}
```

However, compilation fails with:

```
[ERROR] /C:/Users/ADYADAV/Downloads/proj/proj/src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java:[5,59] package org.springframework.boot.test.autoconfigure.orm.jpa does not exist
[ERROR] /C:/Users/ADYADAV/Downloads/proj/proj/src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java:[13,2] cannot find symbol symbol: class DataJpaTest
```

## Root Cause

The `pom.xml` contains an incorrect test dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

This artifact does NOT include `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` (the `@DataJpaTest` annotation).

The correct dependency should be:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

## Constraint Conflict

- **Task Instruction:** "do not change pom.xml"
- **Task Requirement:** Test using `@DataJpaTest` (from spring-boot-starter-test, not webmvc-test)
- **Current State:** pom.xml has wrong test starter, cannot compile required test

## What Was Completed

### Files Created (but not tested):

1. **DocumentRepositoryTest.java** - Created at `src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java`
   - Implements exact test from task-3-brief.md
   - Status: Compilation fails due to missing @DataJpaTest annotation class

2. **DocumentRepository.java** - Created at `src/main/java/project2/example/proj/repository/DocumentRepository.java`
   - Correct JpaRepository interface implementation
   - Status: Compiles successfully, but cannot be tested

### Test Results

```
[ERROR] COMPILATION ERROR
[ERROR] /C:/Users/ADYADAV/Downloads/proj/proj/src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java:[5,59] package org.springframework.boot.test.autoconfigure.orm.jpa does not exist
[ERROR] /C:/Users/ADYADAV/Downloads/proj/proj/src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java:[13,2] cannot find symbol symbol: class DataJpaTest
[INFO] 2 errors
```

## Required Action

To proceed with Task 3, the pom.xml dependency must be corrected in a separate task/step:

Change:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

To:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Once the pom.xml is corrected, Task 3 completion will require:
1. Run failing test (will now compile but fail as expected)
2. Repository already created above
3. Re-run test to verify pass

## Files Status

| File | Status | Notes |
|------|--------|-------|
| `src/main/java/project2/example/proj/repository/DocumentRepository.java` | CREATED | Compiles successfully, awaits test framework fix |
| `src/test/java/project2/example/proj/repository/DocumentRepositoryTest.java` | CREATED | Cannot compile - missing @DataJpaTest annotation class |
| `pom.xml` | INCORRECT | Contains wrong test starter; cannot be modified per task constraints |

## Conclusion

Task 3 implementation is complete from a code perspective, but test execution is blocked by a dependency issue in pom.xml that was established during Task 1. The correct repository interface and test are ready to run once the test dependency is corrected.
