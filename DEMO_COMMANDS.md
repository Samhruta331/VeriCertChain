# VeriCertChain Demo Commands

Use these commands during project review.

## 1. Start Flask Application

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project"
.\run_project.bat
```

Open:

```text
http://127.0.0.1:5000
```

## 2. Show Files Are Stored In IPFS

The project uses the actual `ipfs.exe` already present in your old execution folder.

Start IPFS daemon in a separate PowerShell window:

```powershell
cd "D:\projects\Veri cert chain model\PROJECT PART-2\Execution"
.\ipfs.exe init
.\ipfs.exe daemon
```

In another PowerShell window, add an uploaded certificate file to IPFS:

```powershell
cd "D:\projects\Veri cert chain model\PROJECT PART-2\Execution"
.\ipfs.exe add "C:\Users\SAMHRUTA\Desktop\major project\uploads\VCERT001_v1_Sem_1.png"
```

The command returns a CID like:

```text
added Qm... VCERT001_v1_Sem_1.png
```

This proves the certificate file is stored in IPFS. In the application, the CID is stored with the certificate metadata.

If your uploaded file name is different, list files first:

```powershell
dir "C:\Users\SAMHRUTA\Desktop\major project\uploads"
```

Then replace the file path in the `ipfs add` command.

## 3. Show Sensitive Information Is In Corda Blockchain Code

Open the Corda project:

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project\corda-vericertchain"
```

Show the Corda state:

```powershell
type contracts\src\main\java\com\template\states\CertificateState.java
```

This file contains blockchain metadata fields:

- certificate ID
- student name
- course name
- certificate hash
- IPFS CID
- version
- status
- shard ID
- revocation reason
- issuer
- owner

Show the Corda contract:

```powershell
type contracts\src\main\java\com\template\contracts\CertificateContract.java
```

This proves validation rules are implemented for:

- issue
- version update
- revoke
- issuer signature

Show the Corda flows:

```powershell
type workflows\src\main\java\com\template\flows\IssueCertificateFlow.java
type workflows\src\main\java\com\template\flows\VersionCertificateFlow.java
type workflows\src\main\java\com\template\flows\RevokeCertificateFlow.java
```

## 4. Build The Real Corda CorDapp

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project\corda-vericertchain"
.\gradlew.bat clean build
```

Expected result:

```text
BUILD SUCCESSFUL
```

This proves the Corda CorDapp source compiles successfully.

## 5. Corda Node Deployment Command

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project\corda-vericertchain"
.\gradlew.bat deployNodes
```

If Java/Quasar runtime error appears, say:

```text
The CorDapp build is successful. Live Corda node deployment requires the matching Corda Java runtime. The implementation files, contracts and flows are completed and build verified.
```

## 6. Corda Flow Commands For Live Node Demo

After live nodes are running, use PartyA shell.

Issue:

```text
flow start IssueCertificateFlow certId: VCERT001, studentName: Samhruta, courseName: B.Tech CSE, certificateHash: abc123, cid: CID-abc123, version: 1, shardId: Shard 1, owner: "O=PartyB,L=New York,C=US"
```

Version:

```text
flow start VersionCertificateFlow certId: VCERT001, certificateHash: def456, cid: CID-def456, shardId: Shard 2
```

Revoke:

```text
flow start RevokeCertificateFlow certId: VCERT001, reason: "Certificate cancelled by institution"
```

## 7. Show Performance Metrics In Command Prompt

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project"
.\run_performance_analysis.bat
```

It prints:

- Traditional Centralized time
- IPFS + Hash Only time
- VeriCertChain Sharding time
- Improvement percentage
- Parallel shard timings

The output is also saved here:

```text
C:\Users\SAMHRUTA\Desktop\major project\results\performance_metrics.json
```
