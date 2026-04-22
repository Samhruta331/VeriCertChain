# VeriCertChain Real Corda Execution

This folder contains the real Corda CorDapp setup for VeriCertChain. It is pinned to Corda 4.9 so it can run with the JDK 11 installation available on this system.

Implemented Corda components:

- `CertificateState`
- `CertificateContract`
- `IssueCertificateFlow`
- `VersionCertificateFlow`
- `RevokeCertificateFlow`
- Corda Notary through the deployed network configuration

## Build Corda CorDapp

Open PowerShell in this folder:

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project\corda-vericertchain"
.\gradlew.bat clean build
```

Verified status:

```text
BUILD SUCCESSFUL
```

## Important Java Runtime Note

This system currently has JDK 11 installed:

```text
openjdk version "11.0.30"
```

The CorDapp source compiles successfully, but local node deployment can fail at the Quasar runtime instrumentation stage on this Java setup.

If your review requires running live Corda nodes, install one of these and rerun `deployNodes`:

- JDK 8 for Corda 4.9 node runtime, or
- JDK 17 and change `constants.properties` back to Corda 4.12 / platform 12

For the project submission folder, the implemented Corda source is included and the build is verified.

## Deploy Corda Nodes

```powershell
.\gradlew.bat deployNodes
```

Generated nodes will be available in:

```text
build\nodes
```

The configured network contains:

- Notary
- PartyA
- PartyB

## Start Nodes

Open each node folder inside `build\nodes` and run:

```powershell
java -jar corda.jar
```

## Example Flow Commands

From PartyA shell, use PartyB as the certificate owner.

Issue certificate:

```text
flow start IssueCertificateFlow certId: VCERT001, studentName: Samhruta, courseName: B.Tech CSE, certificateHash: abc123, cid: CID-abc123, version: 1, shardId: Shard 1, owner: "O=PartyB,L=New York,C=US"
```

Create new version:

```text
flow start VersionCertificateFlow certId: VCERT001, certificateHash: def456, cid: CID-def456, shardId: Shard 2
```

Revoke certificate:

```text
flow start RevokeCertificateFlow certId: VCERT001, reason: "Certificate cancelled by institution"
```

The flows print command-prompt metrics like:

```text
CORDA_METRIC issue_certificate_ms=...
CORDA_METRIC version_certificate_ms=...
CORDA_METRIC revoke_certificate_ms=...
```
