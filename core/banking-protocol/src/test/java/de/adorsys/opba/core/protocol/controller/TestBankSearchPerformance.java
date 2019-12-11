package de.adorsys.opba.core.protocol.controller;

import de.adorsys.opba.core.protocol.BaseMockitoTest;
import de.adorsys.opba.core.protocol.dto.TestResult;
import de.adorsys.opba.core.protocol.services.StatisticService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Slf4j
@Testcontainers
class TestBankSearchPerformance extends BaseMockitoTest {

    private static final int POSTGRES_PORT = 5432;
    private static final int N_THREADS = 10;
    private static final int ITERATIONS = N_THREADS * 100;

    private final CountDownLatch latch = new CountDownLatch(ITERATIONS);
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicInteger counterFTS = new AtomicInteger();

    private static List<String> searchStrings = generateTestData();

    private MockMvc mockMvc;

    @Container
    @SuppressWarnings("PMD.UnusedPrivateField")
    private static final DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yml"))
                    .withExposedService("postgres", 1, POSTGRES_PORT, Wait.forListeningPort());

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void onSetUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void TestBankSearch() throws Exception {
        StatisticService statisticService = new StatisticService();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);

        log.info("Starting test endpoints...");
        Runnable runnable = getBanksEndpoint(statisticService, searchStrings);
        searchStrings.forEach(s -> executorService.execute(runnable));

        latch.await();
        printResults(statisticService);
    }

    @Test
    void TestBankSearchFTS() throws Exception {
        StatisticService statisticService = new StatisticService();
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);

        log.info("Starting test FTS endpoints...");
        Runnable runnableFTS = getBanksFTSEndpoint(statisticService, searchStrings);

        searchStrings.forEach(s -> executorService.execute(runnableFTS));

        latch.await();
        printResults(statisticService);
    }

    private void printResults(StatisticService statisticService) {
        log.info("Test results:");
        statisticService.getTestResult().forEach(tr -> log.info("{}", tr));

        Long start = statisticService.getTestResult()
                .stream().map(TestResult::getStart).collect(Collectors.toList())
                .stream().min(Long::compareTo).get();
        Long end = statisticService.getTestResult()
                .stream().map(TestResult::getEnd).collect(Collectors.toList())
                .stream().max(Long::compareTo).get();
        log.info("start: {}", start);
        log.info("end: {}", end);
        log.info("{} calls completed in {} milliseconds", ITERATIONS, end - start);
        log.info("Operations per second: {}", ITERATIONS / ((end - start)/1000));
    }

    @NotNull
    private Runnable getBanksEndpoint(StatisticService statisticService, List<String> searchStrings) {
        return () -> {
            int cnt = counter.incrementAndGet();
            String searchString = searchStrings.get(cnt - 1);
            long start = System.currentTimeMillis();
            try {
                MvcResult mvcResult = mockMvc.perform(
                        get("/v1/banks")
                                .param("q", searchString)
                                .param("max_results", "10"))
                        .andExpect(status().isOk())
                        .andReturn();
                long end = System.currentTimeMillis();
                TestResult testResult = new TestResult(start, end, searchString, mvcResult.getResponse().getContentAsString());
                statisticService.getTestResult().add(testResult);
                latch.countDown();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };
    }

    @NotNull
    private Runnable getBanksFTSEndpoint(StatisticService statisticService, List<String> searchStrings) {
        return () -> {
            int cnt = counterFTS.incrementAndGet();
            String searchString = searchStrings.get(cnt - 1);
            long start = System.currentTimeMillis();
            try {
                MvcResult mvcResult = mockMvc.perform(
                        get("/v1/banks/fts")
                                .param("q", "deu")
                                .param("max_results", "10"))
                        .andExpect(status().isOk())
                        .andReturn();
                long end = System.currentTimeMillis();
                TestResult testResult = new TestResult(start, end, searchString, mvcResult.getResponse().getContentAsString());
                statisticService.getTestResult().add(testResult);
                latch.countDown();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        };
    }

    @NotNull
    private static List<String> generateTestData() {
        log.info("Generating test data...");
        List<String> searchStrings = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            int charCount = new Random().nextInt(5) + 3; // [3..7]
            searchStrings.add(RandomStringUtils.randomAlphabetic(charCount));
        }
        return searchStrings;
    }

}
