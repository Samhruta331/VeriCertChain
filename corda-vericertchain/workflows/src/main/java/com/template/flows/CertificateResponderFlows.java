package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveFinalityFlow;

public class CertificateResponderFlows {

    @InitiatedBy(IssueCertificateFlow.class)
    public static class IssueResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public IssueResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws net.corda.core.flows.FlowException {
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }

    @InitiatedBy(VersionCertificateFlow.class)
    public static class VersionResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public VersionResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws net.corda.core.flows.FlowException {
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }

    @InitiatedBy(RevokeCertificateFlow.class)
    public static class RevokeResponder extends FlowLogic<Void> {
        private final FlowSession counterpartySession;

        public RevokeResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws net.corda.core.flows.FlowException {
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
