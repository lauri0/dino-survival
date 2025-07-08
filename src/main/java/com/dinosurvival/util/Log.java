package com.dinosurvival.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private final Path path;

    public Log(Path path) {
        this.path = path;
    }

    public void append(String text) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Files.writeString(path, timestamp + " " + text + System.lineSeparator(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
