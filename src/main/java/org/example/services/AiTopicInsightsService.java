package org.example.services;

import org.example.entities.Topic;
import org.example.integrations.huggingface.HuggingFaceInferenceClient;
import org.example.utils.EnvConfig;

import java.io.IOException;

public class AiTopicInsightsService {
    private final HuggingFaceInferenceClient hf = new HuggingFaceInferenceClient();

    public AiResult summarizeAndTag(Topic topic) throws IOException {
        if (topic == null) {
            throw new IllegalArgumentException("topic is required");
        }
        String text = buildInput(topic);
        if (text.isBlank()) {
            throw new IllegalArgumentException("topic content is empty");
        }

        String summaryModel = EnvConfig.getOptional("HF_SUMMARY_MODEL", "facebook/bart-large-cnn");
        String tagsModel = EnvConfig.getOptional("HF_TAGS_MODEL", "facebook/bart-large-mnli");

        String summary = hf.summarize(summaryModel, text);

        String[] candidateLabels = new String[]{
                "bug",
                "question",
                "help",
                "java",
                "javafx",
                "mysql",
                "api",
                "security",
                "feature-request",
                "feedback"
        };
        HuggingFaceInferenceClient.TagResult tags = hf.classifyTags(tagsModel, text, candidateLabels);
        return new AiResult(summary, tags.labelsJsonArray(), tags.scoresJsonArray());
    }

    private static String buildInput(Topic topic) {
        String title = topic.getTitle() == null ? "" : topic.getTitle().trim();
        String content = topic.getContent() == null ? "" : topic.getContent().trim();
        if (title.isEmpty()) {
            return content;
        }
        if (content.isEmpty()) {
            return title;
        }
        return "Title: " + title + "\n\nContent: " + content;
    }

    public record AiResult(String summary, String tagsLabelsJson, String tagsScoresJson) {}
}

