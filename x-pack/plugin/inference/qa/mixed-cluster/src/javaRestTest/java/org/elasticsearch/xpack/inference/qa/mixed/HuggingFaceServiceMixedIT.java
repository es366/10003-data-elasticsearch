/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.inference.qa.mixed;

import org.elasticsearch.Version;
import org.elasticsearch.common.Strings;
import org.elasticsearch.inference.TaskType;
import org.elasticsearch.test.http.MockResponse;
import org.elasticsearch.test.http.MockWebServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.xpack.inference.qa.mixed.MixedClusterSpecIT.bwcVersion;
import static org.hamcrest.Matchers.*;

public class HuggingFaceServiceMixedIT extends BaseMixedIT {

    private static final String HF_EMBEDDINGS_ADDED = "8.12.0";
    private static final String HF_ELSER_ADDED = "8.12.0";

    private static MockWebServer embeddingsServer;
    private static MockWebServer elserServer;

    @BeforeClass
    public static void startWebServer() throws IOException {
        embeddingsServer = new MockWebServer();
        embeddingsServer.start();

        elserServer = new MockWebServer();
        elserServer.start();
    }

    @AfterClass
    public static void shutdown() {
        embeddingsServer.close();
        elserServer.close();
    }

    @SuppressWarnings("unchecked")
    public void testHFEmbeddings() throws IOException {
        var embeddingsSupported = bwcVersion.onOrAfter(Version.fromString(HF_EMBEDDINGS_ADDED));
        assumeTrue("Hugging Face embedding service added in " + HF_EMBEDDINGS_ADDED, embeddingsSupported);

        final String oldClusterId = "old-cluster-embeddings";

        embeddingsServer.enqueue(new MockResponse().setResponseCode(200).setBody(embeddingResponse()));
        put(oldClusterId, embeddingConfig(getUrl(embeddingsServer)), TaskType.TEXT_EMBEDDING);
        var configs = (List<Map<String, Object>>) get(TaskType.TEXT_EMBEDDING, oldClusterId).get("endpoints");
        assertThat(configs, hasSize(1));
        assertEquals("hugging_face", configs.get(0).get("service"));
        assertEmbeddingInference(oldClusterId);
    }

    void assertEmbeddingInference(String inferenceId) throws IOException {
        embeddingsServer.enqueue(new MockResponse().setResponseCode(200).setBody(embeddingResponse()));
        var inferenceMap = inference(inferenceId, TaskType.TEXT_EMBEDDING, "some text");
        assertThat(inferenceMap.entrySet(), not(empty()));
    }

    @SuppressWarnings("unchecked")
    public void testElser() throws IOException {
        var supported = bwcVersion.onOrAfter(Version.fromString(HF_ELSER_ADDED));
        assumeTrue("HF elser service added in " + HF_ELSER_ADDED, supported);

        final String oldClusterId = "old-cluster-elser";
        final String upgradedClusterId = "upgraded-cluster-elser";

        put(oldClusterId, elserConfig(getUrl(elserServer)), TaskType.SPARSE_EMBEDDING);

        var configs = (List<Map<String, Object>>) get(TaskType.SPARSE_EMBEDDING, oldClusterId).get("endpoints");
        assertThat(configs, hasSize(1));
        assertEquals("hugging_face", configs.get(0).get("service"));
        assertElser(oldClusterId);
    }

    private void assertElser(String inferenceId) throws IOException {
        elserServer.enqueue(new MockResponse().setResponseCode(200).setBody(elserResponse()));
        var inferenceMap = inference(inferenceId, TaskType.SPARSE_EMBEDDING, "some text");
        assertThat(inferenceMap.entrySet(), not(empty()));
    }

    private String embeddingConfig(String url) {
        return Strings.format("""
            {
                "service": "hugging_face",
                "service_settings": {
                    "url": "%s",
                    "api_key": "XXXX"
                }
            }
            """, url);
    }

    private String embeddingResponse() {
        return """
            [
                  [
                      0.014539449,
                      -0.015288644
                  ]
            ]
            """;
    }

    private String elserConfig(String url) {
        return Strings.format("""
            {
                "service": "hugging_face",
                "service_settings": {
                    "api_key": "XXXX",
                    "url": "%s"
                }
            }
            """, url);
    }

    private String elserResponse() {
        return """
            [
                {
                    ".": 0.133155956864357,
                    "the": 0.6747211217880249
                }
            ]
            """;
    }

}
