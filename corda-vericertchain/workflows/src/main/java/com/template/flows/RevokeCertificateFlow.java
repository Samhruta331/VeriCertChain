package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.CertificateContract;
import com.template.states.CertificateState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Collections;
import java.util.Comparator;

@InitiatingFlow
@StartableByRPC
public class RevokeCertificateFlow extends FlowLogic<SignedTransaction> {

    private final String certId;
    private final String reason;

    public RevokeCertificateFlow(String certId, String reason) {
        this.certId = certId;
        this.reason = reason;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        long startTime = System.nanoTime();
        StateAndRef<CertificateState> inputRef = findLatestActiveState(certId);
        CertificateState input = inputRef.getState().getData();

        CertificateState output = new CertificateState(
                input.getCertId(),
                input.getStudentName(),
                input.getCourseName(),
                input.getCertificateHash(),
                input.getCid(),
                input.getVersion(),
                "revoked",
                input.getShardId(),
                reason,
                input.getIssuer(),
                input.getOwner(),
                input.getLinearId()
        );

        Command<CertificateContract.Commands.Revoke> command =
                new Command<>(new CertificateContract.Commands.Revoke(), getOurIdentity().getOwningKey());

        TransactionBuilder txBuilder = new TransactionBuilder(inputRef.getState().getNotary())
                .addInputState(inputRef)
                .addOutputState(output, CertificateContract.ID)
                .addCommand(command);

        txBuilder.verify(getServiceHub());
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession session = initiateFlow(input.getOwner());
        SignedTransaction finalTx = subFlow(new FinalityFlow(signedTx, Collections.singletonList(session)));

        long executionTime = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("CORDA_METRIC revoke_certificate_ms=" + executionTime);
        return finalTx;
    }

    private StateAndRef<CertificateState> findLatestActiveState(String certId) throws FlowException {
        return getServiceHub().getVaultService().queryBy(CertificateState.class).getStates().stream()
                .filter(state -> state.getState().getData().getCertId().equals(certId))
                .filter(state -> !"revoked".equals(state.getState().getData().getStatus()))
                .max(Comparator.comparingInt(state -> state.getState().getData().getVersion()))
                .orElseThrow(() -> new FlowException("Active certificate not found: " + certId));
    }
}
