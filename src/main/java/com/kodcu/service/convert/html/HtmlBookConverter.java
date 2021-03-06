package com.kodcu.service.convert.html;

import com.kodcu.component.HtmlPane;
import com.kodcu.controller.ApplicationController;
import com.kodcu.other.Current;
import com.kodcu.other.IOHelper;
import com.kodcu.service.DirectoryService;
import com.kodcu.service.convert.markdown.MarkdownService;
import com.kodcu.service.ThreadService;
import com.kodcu.service.convert.DocumentConverter;
import com.kodcu.service.convert.Traversable;
import com.kodcu.service.ui.IndikatorService;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Created by usta on 30.08.2014.
 */
@Component
public class HtmlBookConverter implements Traversable,DocumentConverter<String> {

    private final Logger logger = LoggerFactory.getLogger(HtmlBookConverter.class);

    private final Pattern compiledRegex = Pattern.compile("(?<=include::)(.*?)(?=\\[(.*?)\\])");

    private final ApplicationController controller;
    private final ThreadService threadService;
    private final DirectoryService directoryService;
    private final Current current;
    private final IndikatorService indikatorService;
    private final MarkdownService markdownService;
    private final HtmlPane htmlPane;

    private Path htmlBookPath;

    @Autowired
    public HtmlBookConverter(final ApplicationController controller, final ThreadService threadService,
                             final DirectoryService directoryService, final Current current,
                             IndikatorService indikatorService, MarkdownService markdownService, HtmlPane htmlPane) {
        this.controller = controller;
        this.threadService = threadService;
        this.directoryService = directoryService;
        this.current = current;
        this.indikatorService = indikatorService;
        this.markdownService = markdownService;
        this.htmlPane = htmlPane;
    }

    @Override
    public void convert(boolean askPath, Consumer<String>... nextStep) {

        try {

            final Path currentTabPath = current.currentPath().get();
            final Path currentTabPathDir = currentTabPath.getParent();
            final String tabText = current.getCurrentTabText().replace("*", "").trim();

            final String asciidoc = current.currentEditorValue();

                threadService.runActionLater(() -> {

                    String rendered = htmlPane.convertHtml(asciidoc).getRendered();

                    if (askPath) {
                        final FileChooser fileChooser = directoryService.newFileChooser("Save HTML file");
                        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("HTML", "*.html"));
                        htmlBookPath = fileChooser.showSaveDialog(null).toPath();
                    } else
                        htmlBookPath = currentTabPathDir.resolve(tabText + ".html");

                    indikatorService.startCycle();
                    logger.debug("HTML conversion started");

                    IOHelper.writeToFile(htmlBookPath, rendered, CREATE, TRUNCATE_EXISTING);

                    threadService.runActionLater(() -> {
                        controller.getRecentFilesList().remove(htmlBookPath.toString());
                        controller.getRecentFilesList().add(0, htmlBookPath.toString());
                    });

                    indikatorService.completeCycle();
                    logger.debug("HTML conversion ended");
                });
        } catch (Exception e) {
            logger.error("Problem occured while converting to HTML", e);
        } finally {
            indikatorService.completeCycle();
        }

    }
}
