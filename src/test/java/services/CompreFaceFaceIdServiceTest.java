package com.esprit.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompreFaceFaceIdServiceTest {

    @AfterEach
    void clearThresholdOverride() {
        System.clearProperty("COMPREFACE_SIMILARITY_THRESHOLD");
    }

    @Test
    void shouldReturnMatchWhenSubjectAndSimilarityAreValid() {
        System.setProperty("COMPREFACE_SIMILARITY_THRESHOLD", "0.70");
        String response = "{\"result\":[{\"subjects\":[{\"subject\":\"user@example.com\",\"similarity\":0.91}]}]}";

        CompreFaceFaceIdService.VerificationResult result =
                CompreFaceFaceIdService.parseVerifyResponse("user@example.com", response);

        assertEquals(CompreFaceFaceIdService.VerificationStatus.MATCH, result.status());
    }

    @Test
    void shouldReturnNoFaceWhenResultArrayIsEmpty() {
        String response = "{\"result\":[]}";

        CompreFaceFaceIdService.VerificationResult result =
                CompreFaceFaceIdService.parseVerifyResponse("user@example.com", response);

        assertEquals(CompreFaceFaceIdService.VerificationStatus.NO_FACE, result.status());
    }

    @Test
    void shouldReturnNoMatchWhenSimilarityIsTooLow() {
        System.setProperty("COMPREFACE_SIMILARITY_THRESHOLD", "0.95");
        String response = "{\"result\":[{\"subjects\":[{\"subject\":\"user@example.com\",\"similarity\":0.70}]}]}";

        CompreFaceFaceIdService.VerificationResult result =
                CompreFaceFaceIdService.parseVerifyResponse("user@example.com", response);

        assertEquals(CompreFaceFaceIdService.VerificationStatus.NO_MATCH, result.status());
    }

    @Test
    void shouldRecognizeSubjectWhenSimilarityIsAboveThreshold() {
        System.setProperty("COMPREFACE_SIMILARITY_THRESHOLD", "0.65");
        String response = "{\"result\":[{\"subjects\":[{\"subject\":\"usr_abc\",\"similarity\":0.88}]}]}";

        CompreFaceFaceIdService.RecognitionResult result =
                CompreFaceFaceIdService.parseRecognizeResponse(response);

        assertEquals(CompreFaceFaceIdService.RecognitionStatus.MATCH, result.status());
        assertEquals("usr_abc", result.subject());
    }

    @Test
    void shouldReturnNoFaceForRecognitionWhenNoFaceDetected() {
        String response = "{\"result\":[]}";

        CompreFaceFaceIdService.RecognitionResult result =
                CompreFaceFaceIdService.parseRecognizeResponse(response);

        assertEquals(CompreFaceFaceIdService.RecognitionStatus.NO_FACE, result.status());
    }
}



