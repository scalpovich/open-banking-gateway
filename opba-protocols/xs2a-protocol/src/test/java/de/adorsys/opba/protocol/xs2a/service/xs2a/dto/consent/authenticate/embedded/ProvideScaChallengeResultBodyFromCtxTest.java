package de.adorsys.opba.protocol.xs2a.service.xs2a.dto.consent.authenticate.embedded;

import de.adorsys.opba.protocol.xs2a.config.MapperTestConfig;
import de.adorsys.opba.protocol.xs2a.service.xs2a.context.Xs2aContext;
import de.adorsys.opba.protocol.xs2a.util.UtilService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MapperTestConfig.class)
public class ProvideScaChallengeResultBodyFromCtxTest {
    public static final String PATH_PREFIX = "mapper-test-fixtures/provide_sca_challenge_result_body_from_";

    @Autowired
    private ProvideScaChallengeResultBody.FromCtx mapper;
    @Autowired
    private UtilService utilService;

    @Test
    @SneakyThrows
    public void provideScaChallengeResultBodyFromCtxMapperTest() {
        // Given
        Xs2aContext mappingInput = utilService.getFromFile(PATH_PREFIX + "xs2a_context_input.json", Xs2aContext.class);
        ProvideScaChallengeResultBody expected = utilService.getFromFile(PATH_PREFIX + "xs2a_context_output.json", ProvideScaChallengeResultBody.class);

        // When
        ProvideScaChallengeResultBody actual = mapper.map(mappingInput);

        // Then
        assertThat(expected).isEqualToComparingFieldByField(actual);
    }
}