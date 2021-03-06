package com.kodcu.service.convert.slide;

import com.kodcu.component.HtmlPane;
import com.kodcu.component.SlidePane;
import com.kodcu.controller.ApplicationController;
import com.kodcu.other.Current;
import com.kodcu.service.convert.markdown.MarkdownService;
import com.kodcu.service.ThreadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by usta on 09.04.2015.
 */
@Component
public class SlideConverter  {

    private final ApplicationController controller;
    private final ThreadService threadService;
    private final MarkdownService markdownService;
    private final Current current;
    private final ApplicationContext applicationContext;
    private final HtmlPane htmlPane;
    private final SlidePane slidePane;
    private final Pattern pattern = Pattern.compile(":slide-type:.*(deckjs|revealjs)", Pattern.MULTILINE);
    private String rendered;


    @Autowired
    public SlideConverter(final ApplicationController controller, final ThreadService threadService, final MarkdownService markdownService, final Current current, ApplicationContext applicationContext, HtmlPane htmlPane, SlidePane slidePane) {
        this.controller = controller;
        this.threadService = threadService;
        this.markdownService = markdownService;
        this.current = current;
        this.applicationContext = applicationContext;
        this.htmlPane = htmlPane;
        this.slidePane = slidePane;
    }

    public String currentType() {
        String input = current.currentEditorValue();
        Matcher matcher = pattern.matcher(input);

        String slideType = "revealjs";

        if (matcher.find()) {
            slideType = matcher.group(1);
        }
        return slideType;
    }

    public void convert(String rendered, Consumer<String>... nextStep) {
        threadService.runActionLater(() -> {

            this.rendered = rendered;

            if (Objects.isNull(slidePane.getLocation())) {
                slidePane.load(String.format("http://localhost:%d/slide/index.slide", controller.getPort()));
                slidePane.injectExtensions();
            } else {
                threadService.runActionLater(() -> {
                    slidePane.replaceSlides(rendered);
                });
            }

            for (Consumer<String> step : nextStep) {
                step.accept(rendered);
            }
        });
    }

    public String getRendered() {
        return rendered;
    }
}
