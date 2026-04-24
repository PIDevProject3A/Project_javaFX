package com.bledna.util;

import java.io.File;

public interface AIService {
    /**
     * Analyzes an image and returns a JSON string or formatted string 
     * containing waste type and recycling advice.
     */
    String analyzeWaste(File imageFile);
}
