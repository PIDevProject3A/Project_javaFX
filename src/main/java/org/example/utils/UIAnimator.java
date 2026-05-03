package org.example.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public final class UIAnimator {

    private UIAnimator() {
    }

    /**
     * Animate a node's entrance by fading it in and translating it up.
     * @param node The JavaFX Node to animate.
     * @param delayMs The delay before the animation starts (useful for staggered lists).
     */
    public static void animateEntrance(Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(20);

        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(400), node);
        translate.setFromY(20);
        translate.setToY(0);

        ParallelTransition pt = new ParallelTransition(fade, translate);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    /**
     * Add a slight pop/scale effect when the mouse hovers over a node (e.g. Button).
     * @param node The JavaFX Node to apply the effect to.
     */
    public static void addHoverScaleEffect(Node node) {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(150), node);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), node);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        node.setOnMouseEntered(e -> {
            scaleOut.stop();
            scaleIn.playFromStart();
        });

        node.setOnMouseExited(e -> {
            scaleIn.stop();
            scaleOut.playFromStart();
        });
    }
}
