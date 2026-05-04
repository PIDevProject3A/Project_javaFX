package org.example.utils;

import javafx.util.StringConverter;
import org.example.entities.TopicStatus;

/** Shows {@link TopicStatus} in {@link javafx.scene.control.ComboBox} with plain English labels. */
public final class TopicStatusStringConverter extends StringConverter<TopicStatus> {

    @Override
    public String toString(TopicStatus object) {
        return object == null ? "" : object.getDisplayLabel();
    }

    @Override
    public TopicStatus fromString(String string) {
        TopicStatus byLabel = TopicStatus.fromDisplayLabel(string);
        return byLabel != null ? byLabel : TopicStatus.fromDb(string);
    }
}
