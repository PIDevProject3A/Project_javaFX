package org.example.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import org.example.entities.TopicStatus;

/** Topic status {@link ComboBox} setup (visible text + dropdown cells). */
public final class TopicStatusComboHelper {

    private TopicStatusComboHelper() {
    }

    public static void setup(ComboBox<TopicStatus> combo) {
        combo.setConverter(new TopicStatusStringConverter());
        combo.setButtonCell(statusListCell());
        combo.setCellFactory(lv -> statusListCell());
    }

    private static ListCell<TopicStatus> statusListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(TopicStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayLabel());
                }
            }
        };
    }
}
