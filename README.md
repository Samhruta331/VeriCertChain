# VeriCertChain Major Project

VeriCertChain is a Flask based certificate verification system. It demonstrates certificate issuing, SHA-256 hashing, QR based verification, blockchain style ledger storage, revocation, duplicate detection and audit logs.

This repository also includes a real Corda CorDapp implementation under `corda-vericertchain/` for certificate state, contract validation, issue/version/revoke flows, and Corda Notary-based transaction uniqueness.

## Modules

- Institution dashboard
- Certificate issuing
- QR code generation
- Blockchain ledger simulation
- Certificate verification
- Certificate revocation
- Certificate versioning
- Shard allocation and load balancing
- Duplicate/fraud detection
- Audit logs

## How to Run

1. Open Command Prompt or PowerShell.
2. Go to the project folder:

```powershell
cd "C:\Users\SAMHRUTA\Desktop\major project"
```

3. Create a virtual environment:

```powershell
python -m venv venv
```

4. Activate the virtual environment:

```powershell
venv\Scripts\activate
```

5. Install required packages:

```powershell
pip install -r requirements.txt
```

6. Start the application:

```powershell
python app.py
```

7. Open this URL in the browser:

```text
http://127.0.0.1:5000
```

## Demo Commands

All review/demo commands are collected in:

```text
DEMO_COMMANDS.md
VeriCertChain_Demo_Commands.docx
```

## Execution Flow

1. The institution opens the Issue page and uploads a certificate.
2. The system calculates the SHA-256 hash of the uploaded certificate.
3. A simulated IPFS CID is generated from the certificate hash.
4. A QR code is generated for the certificate verification link.
5. Certificate data is stored in `database.json`.
6. A blockchain style block is added to `blockchain.json`.
7. Verifiers can enter the certificate ID or scan the QR code.
8. If the same certificate ID is issued again, the system creates a new version and keeps the earlier version in version history.
9. The system recalculates the certificate hash and compares it with the stored hash.
10. The result is shown as valid, revoked, duplicate, tampered or not found.
11. Each important activity is stored in `audit_logs.json`.

## Project Files

- `app.py` - Main Flask application
- `templates/` - Web pages
- `static/css/style.css` - Styling
- `static/qr/` - Generated QR codes
- `uploads/` - Uploaded certificate files
- `database.json` - Certificate records
- `blockchain.json` - Blockchain style ledger
- `audit_logs.json` - Audit history
- `corda-vericertchain/` - Real Corda CorDapp implementation
- `performance_analysis.py` - Command prompt performance comparison script
- `run_performance_analysis.bat` - Launcher for result analysis
- `DEMO_COMMANDS.md` - Ready commands for project review

## Implemented Algorithms

- SHA-256 certificate hashing
- Metadata-based shard allocation
- Shard load balancing
- Corda notary style uniqueness simulation
- Metadata-driven versioning
- Reason-tagged revocation
- QR-based verification
- Hash consistency fraud detection

## Result Analysis

Run this command from the project folder:

```powershell
run_performance_analysis.bat
```

It prints average execution time for:

- Traditional centralized model
- IPFS + hash only model
- VeriCertChain shard-aware model

It also saves the output in:

```text
results/performance_metrics.json
```

## Real Corda Status

The project includes a real Corda CorDapp in:

```text
corda-vericertchain/
```

Implemented Corda elements:

- `CertificateState`
- `CertificateContract`
- `IssueCertificateFlow`
- `VersionCertificateFlow`
- `RevokeCertificateFlow`
- Corda Notary-based uniqueness through deployed node configuration

The CorDapp was verified with:

```powershell
.\gradlew.bat clean build
```

The build completed successfully. Live node deployment may require installing the matching Corda Java runtime as described in `corda-vericertchain/VERICERTCHAIN_CORDA_EXECUTION.md`.
