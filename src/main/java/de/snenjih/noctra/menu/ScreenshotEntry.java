package de.snenjih.noctra.menu;

import java.nio.file.Path;

public record ScreenshotEntry(Path path, String filename, long lastModified) {}
