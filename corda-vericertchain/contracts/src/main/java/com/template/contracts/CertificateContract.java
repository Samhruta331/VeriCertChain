package com.template.contracts;

import com.template.states.CertificateState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CertificateContract implements Contract {

    public static final String ID = "com.template.contracts.CertificateContract";

    @Override
    public void verify(LedgerTransaction tx) {
        CommandData command = tx.getCommands().get(0).getValue();

        if (command instanceof Commands.Issue) {
            verifyIssue(tx);
        } else if (command instanceof Commands.Version) {
            verifyVersion(tx);
        } else if (command instanceof Commands.Revoke) {
            verifyRevoke(tx);
        } else {
            throw new IllegalArgumentException("Unsupported certificate command.");
        }
    }

    private void verifyIssue(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("Issue should not consume input states.", tx.getInputStates().isEmpty());
            require.using("Issue should create one output state.", tx.getOutputStates().size() == 1);
            CertificateState output = tx.outputsOfType(CertificateState.class).get(0);
            require.using("Certificate common fields must be complete.", hasCommonFields(output));
            require.using("First certificate version must be at least 1.", output.getVersion() >= 1);
            require.using("Issued certificate status must be valid.", "valid".equals(output.getStatus()) || "duplicate".equals(output.getStatus()));
            require.using("Issuer must sign the transaction.", hasIssuerSignature(tx, output));
            return null;
        });
    }

    private void verifyVersion(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("Versioning should consume one input state.", tx.getInputStates().size() == 1);
            require.using("Versioning should create one output state.", tx.getOutputStates().size() == 1);
            CertificateState input = tx.inputsOfType(CertificateState.class).get(0);
            CertificateState output = tx.outputsOfType(CertificateState.class).get(0);
            require.using("Certificate common fields must be complete.", hasCommonFields(output));
            require.using("Certificate ID must remain same during versioning.", input.getCertId().equals(output.getCertId()));
            require.using("Version must increase by one.", output.getVersion() == input.getVersion() + 1);
            require.using("Versioned certificate must be valid.", "valid".equals(output.getStatus()));
            require.using("Issuer must sign the transaction.", hasIssuerSignature(tx, output));
            return null;
        });
    }

    private void verifyRevoke(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("Revocation should consume one input state.", tx.getInputStates().size() == 1);
            require.using("Revocation should create one output state.", tx.getOutputStates().size() == 1);
            CertificateState input = tx.inputsOfType(CertificateState.class).get(0);
            CertificateState output = tx.outputsOfType(CertificateState.class).get(0);
            require.using("Certificate ID must remain same during revocation.", input.getCertId().equals(output.getCertId()));
            require.using("Version must not change during revocation.", output.getVersion() == input.getVersion());
            require.using("Revoked certificate status must be revoked.", "revoked".equals(output.getStatus()));
            require.using("Revocation reason must be provided.", output.getRevocationReason() != null && !output.getRevocationReason().isBlank());
            require.using("Issuer must sign the transaction.", hasIssuerSignature(tx, output));
            return null;
        });
    }

    private boolean hasCommonFields(CertificateState output) {
        return output.getCertId() != null && !output.getCertId().isBlank()
                && output.getStudentName() != null && !output.getStudentName().isBlank()
                && output.getCourseName() != null && !output.getCourseName().isBlank()
                && output.getCertificateHash() != null && !output.getCertificateHash().isBlank()
                && output.getCid() != null && !output.getCid().isBlank()
                && output.getShardId() != null && !output.getShardId().isBlank();
    }

    private boolean hasIssuerSignature(LedgerTransaction tx, CertificateState output) {
        List<PublicKey> signers = tx.getCommands().get(0).getSigners();
        return signers.contains(output.getIssuer().getOwningKey());
    }

    public interface Commands extends CommandData {
        class Issue implements Commands {}
        class Version implements Commands {}
        class Revoke implements Commands {}
    }
}
