package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StudyAppDocumentLoaderTest {

    @Resource
    private StudyAppDocumentLoader studyAppDocumentLoader;

    @Test
    void loadMarkdowns() {
        studyAppDocumentLoader.loadMarkdowns();
    }
}
