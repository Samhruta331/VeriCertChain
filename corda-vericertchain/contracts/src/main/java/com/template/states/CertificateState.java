package com.template.states;

import com.template.contracts.CertificateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(CertificateContract.class)
public class CertificateState implements LinearState {

    private final String certId;
    private final String studentName;
    private final String courseName;
    private final String certificateHash;
    private final String cid;
    private final int version;
    private final String status;
    private final String shardId;
    private final String revocationReason;
    private final Party issuer;
    private final Party owner;
    private final UniqueIdentifier linearId;

    public CertificateState(String certId,
                            String studentName,
                            String courseName,
                            String certificateHash,
                            String cid,
                            int version,
                            String status,
                            String shardId,
                            String revocationReason,
                            Party issuer,
                            Party owner,
                            UniqueIdentifier linearId) {
        this.certId = certId;
        this.studentName = studentName;
        this.courseName = courseName;
        this.certificateHash = certificateHash;
        this.cid = cid;
        this.version = version;
        this.status = status;
        this.shardId = shardId;
        this.revocationReason = revocationReason;
        this.issuer = issuer;
        this.owner = owner;
        this.linearId = linearId;
    }

    public String getCertId() {
        return certId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCertificateHash() {
        return certificateHash;
    }

    public String getCid() {
        return cid;
    }

    public int getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public String getShardId() {
        return shardId;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getOwner() {
        return owner;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, owner);
    }
}
