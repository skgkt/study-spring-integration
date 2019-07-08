package com.example.demo;

import java.io.File;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.*;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.PollableChannel;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class).web(WebApplicationType.NONE).run(args);
    }

    @Bean(PollerMetadata.DEFAULT_POLLER)
    public PollerSpec poller() {
        return Pollers.fixedRate(100).maxMessagesPerPoll(1);
    }

    public IntegrationFlow flow() {
        return IntegrationFlows.from(
                        Files.inboundAdapter(new File("in"))
                                .autoCreateDirectory(true)
                                .scanEachPoll(false),
                        e -> e.poller(poller()))
                .<File, File>transform(
                        e -> {
                            log.info("Class: {}", e.getClass());
                            log.info("{}", e.getName());
                            return e;
                        })
                .channel(pollableChannel())
                .get();
    }

    @Autowired IntegrationFlowContext flowContext;

    @Bean
    public PollableChannel pollableChannel() {
        return MessageChannels.queue().get();
    }

    @Autowired ConfigurableApplicationContext ctx;

    @Override
    public void run(String... args) throws Exception {
        IntegrationFlowContext.IntegrationFlowRegistration registration =
                flowContext.registration(flow()).register();
        registration.start();
        log.info("run method.");
        IntStream.range(0, 10)
                .forEachOrdered(i -> log.info("Received: {} {}", i, pollableChannel().receive()));
        ctx.stop();
        ctx.close();
    }
}
