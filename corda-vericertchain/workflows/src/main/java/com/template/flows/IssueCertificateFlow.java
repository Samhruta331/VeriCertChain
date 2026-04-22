package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.CertificateContract;
import com.template.states.CertificateState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Collections;

@InitiatingFlow
@StartableByRPC
public class IssueCertificateFlow extends FlowLogic<SignedTransaction> {

    private final String certId;
    private final String studentName;
    private final String courseName;
    private final String certificateHash;
    private final String cid;
    private final int version;
    private final String shardId;
    private final Party owner;

    public IssueCertificateFlow(String certId,
                                String studentName,
                                String courseName,
                                String certificateHash,
                                String cid,
                                int version,
                                String shardId,
                                Party owner) {
        this.certId = certId;
        this.studentName = studentName;
        this.courseName = courseName;
        this.certificateHash = certificateHash;
        this.cid = cid;
        this.version = version;
        this.shardId = shardId;
        this.owner = owner;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        long startTime = System.nanoTime();
        Party issuer = getOurIdentity();

        CertificateState outputState = new CertificateState(
                certId,
                studentName,
                courseName,
                certificateHash,
                cid,
                version,
                "valid",
                shardId,
                "",
                issuer,
                owner,
                new UniqueIdentifier()
        );

        Command<CertificateContract.Commands.Issue> command =
                new Command<>(new CertificateContract.Commands.Issue(), issuer.getOwningKey());

        TransactionBuilder txBuilder = new TransactionBuilder(
                getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0)
        )
                .addOutputState(outputState, CertificateContract.ID)
                .addCommand(command);

        txBuilder.verify(getServiceHub());
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);
        FlowSession session = initiateFlow(owner);
        SignedTransaction finalTx = subFlow(new FinalityFlow(signedTx, Collections.singletonList(session)));

        long executionTime = (System.nanoTime() - startTime) / 1_000_000;
        System.out.println("CORDA_METRIC issue_certificate_ms=" + executionTime);
        return finalTx;
    }
}
