package de.adorsys.opba.core.protocol.service.xs2a.consent.authenticate.embedded;

import de.adorsys.opba.core.protocol.domain.dto.forms.ScaMethod;
import de.adorsys.opba.core.protocol.service.ContextUtil;
import de.adorsys.opba.core.protocol.service.ValidatedExecution;
import de.adorsys.opba.core.protocol.service.xs2a.context.Xs2aContext;
import de.adorsys.opba.core.protocol.service.xs2a.dto.Xs2aStandardHeaders;
import de.adorsys.opba.core.protocol.service.xs2a.dto.consent.authenticate.embedded.ProvidePsuPassword;
import de.adorsys.opba.core.protocol.service.xs2a.validation.Xs2aValidator;
import de.adorsys.xs2a.adapter.service.AccountInformationService;
import de.adorsys.xs2a.adapter.service.Response;
import de.adorsys.xs2a.adapter.service.model.UpdatePsuAuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service("xs2aAuthenticateUserConsent")
@RequiredArgsConstructor
public class Xs2aAuthenticateUserConsent extends ValidatedExecution<Xs2aContext> {

    private final ProvidePsuPassword.FromCtx toValidatableBody;
    private final ProvidePsuPassword.ToXs2aApi toBody;
    private final Xs2aStandardHeaders.FromCtx toHeaders;
    private final Xs2aValidator validator;
    private final RuntimeService runtimeService;
    private final AccountInformationService ais;

    @Override
    protected void doValidate(DelegateExecution execution, Xs2aContext context) {
        validator.validate(execution, toHeaders.map(context), toValidatableBody.map(context));
    }

    @Override
    protected void doRealExecution(DelegateExecution execution, Xs2aContext context) {
        Response<UpdatePsuAuthenticationResponse> authResponse = ais.updateConsentsPsuData(
                context.getConsentId(),
                context.getAuthorizationId(),
                toHeaders.map(context).toHeaders(),
                toBody.map(toValidatableBody.map(context))
        );

        ContextUtil.getAndUpdateContext(
                execution,
                (Xs2aContext ctx) -> {
                    setScaAvailableMethodsIfCanBeChosen(authResponse, ctx);
                    ctx.setScaSelected(authResponse.getBody().getChosenScaMethod());
                }
        );
    }

    private void setScaAvailableMethodsIfCanBeChosen(
        Response<UpdatePsuAuthenticationResponse> authResponse, Xs2aContext ctx
    ) {
        if (null == authResponse.getBody().getScaMethods()) {
           return;
        }

        ctx.setAvailableSca(
            authResponse.getBody().getScaMethods().stream()
                .map(ScaMethod.FROM_AUTH::map)
                .collect(Collectors.toList())
        );
    }

    @Override
    protected void doMockedExecution(DelegateExecution execution, Xs2aContext context) {
        runtimeService.trigger(execution.getId());
    }
}