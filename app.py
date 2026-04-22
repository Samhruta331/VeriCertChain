from datetime import datetime
import hashlib
import json
import os
from pathlib import Path
from uuid import uuid4

import qrcode
from flask import Flask, redirect, render_template, request, url_for
from werkzeug.utils import secure_filename


app = Flask(__name__)

BASE_DIR = Path(__file__).resolve().parent
UPLOAD_DIR = BASE_DIR / "uploads"
QR_DIR = BASE_DIR / "static" / "qr"
DB_FILE = BASE_DIR / "database.json"
BLOCKCHAIN_FILE = BASE_DIR / "blockchain.json"
AUDIT_FILE = BASE_DIR / "audit_logs.json"
SHARDS = ("Shard 1", "Shard 2", "Shard 3")

for folder in (UPLOAD_DIR, QR_DIR):
    folder.mkdir(parents=True, exist_ok=True)


def read_json(path, default):
    if not path.exists():
        return default
    try:
        with path.open("r", encoding="utf-8") as file:
            return json.load(file)
    except json.JSONDecodeError:
        return default


def write_json(path, data):
    with path.open("w", encoding="utf-8") as file:
        json.dump(data, file, indent=4)


def file_hash(path):
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(4096), b""):
            digest.update(chunk)
    return digest.hexdigest()


def text_hash(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def make_cid(certificate_hash):
    return "CID-" + certificate_hash[:24]


def shard_loads(database):
    loads = {shard: 0 for shard in SHARDS}
    for record in database.values():
        shard_id = record.get("shard_id")
        if shard_id in loads:
            loads[shard_id] += 1
    return loads


def allocate_shard(certificate_id, issuer_name, course_name, database):
    shard_key = f"{issuer_name}|{course_name}|{certificate_id}".lower()
    preferred_index = int(text_hash(shard_key), 16) % len(SHARDS)
    preferred_shard = SHARDS[preferred_index]
    loads = shard_loads(database)
    least_loaded_shard = min(loads, key=loads.get)

    if loads[preferred_shard] > loads[least_loaded_shard] + 1:
        selected_shard = least_loaded_shard
        reason = "Load balancing override"
    else:
        selected_shard = preferred_shard
        reason = "Metadata hash allocation"

    return selected_shard, shard_key, reason


def add_audit(action, certificate_id, message):
    logs = read_json(AUDIT_FILE, [])
    logs.append(
        {
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "action": action,
            "certificate_id": certificate_id,
            "message": message,
        }
    )
    write_json(AUDIT_FILE, logs)


def create_block(record):
    chain = read_json(BLOCKCHAIN_FILE, [])
    previous_hash = chain[-1]["block_hash"] if chain else "0"
    block = {
        "index": len(chain) + 1,
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "event": record.get("event", "ISSUE"),
        "certificate_id": record["certificate_id"],
        "version": record.get("version", 1),
        "shard_id": record.get("shard_id", "Shard 1"),
        "student_name": record["student_name"],
        "certificate_hash": record["certificate_hash"],
        "cid": record["cid"],
        "status": record["status"],
        "previous_hash": previous_hash,
    }
    block["block_hash"] = text_hash(json.dumps(block, sort_keys=True))
    chain.append(block)
    write_json(BLOCKCHAIN_FILE, chain)
    return block


def version_snapshot(record):
    return {
        "version": record.get("version", 1),
        "certificate_hash": record["certificate_hash"],
        "cid": record["cid"],
        "filename": record["filename"],
        "status": record["status"],
        "created_at": record["created_at"],
    }


def normalize_record(record):
    record.setdefault("version", 1)
    record.setdefault("shard_id", "Shard 1")
    record.setdefault("shard_key", record["certificate_id"])
    record.setdefault("shard_reason", "Default allocation")
    record.setdefault("versions", [version_snapshot(record)])
    record.setdefault("revoked_at", None)
    record.setdefault("revocation_reason", None)
    return record


def find_duplicate(certificate_hash):
    database = read_json(DB_FILE, {})
    for certificate_id, record in database.items():
        record = normalize_record(record)
        if record["certificate_hash"] == certificate_hash and record["status"] == "valid":
            return certificate_id
    return None


@app.route("/")
def index():
    database = {key: normalize_record(value) for key, value in read_json(DB_FILE, {}).items()}
    chain = read_json(BLOCKCHAIN_FILE, [])
    logs = read_json(AUDIT_FILE, [])
    loads = shard_loads(database)
    valid_count = sum(1 for item in database.values() if item["status"] == "valid")
    revoked_count = sum(1 for item in database.values() if item["status"] == "revoked")
    return render_template(
        "dashboard.html",
        total=len(database),
        valid_count=valid_count,
        revoked_count=revoked_count,
        block_count=len(chain),
        shard_loads=loads,
        certificates=database,
        logs=logs[-5:],
    )


@app.route("/issue", methods=["GET", "POST"])
def issue_certificate():
    if request.method == "GET":
        return render_template("issue.html")

    uploaded_file = request.files.get("certificate_file")
    if not uploaded_file or uploaded_file.filename == "":
        return render_template("issue.html", error="Please select a certificate file.")

    certificate_id = request.form.get("certificate_id", "").strip() or "CERT-" + uuid4().hex[:8].upper()
    student_name = request.form.get("student_name", "").strip()
    course_name = request.form.get("course_name", "").strip()
    issue_date = request.form.get("issue_date", "").strip()
    issuer_name = request.form.get("issuer_name", "").strip()

    database = {key: normalize_record(value) for key, value in read_json(DB_FILE, {}).items()}
    previous_record = database.get(certificate_id)
    version = previous_record["version"] + 1 if previous_record else 1

    safe_name = secure_filename(uploaded_file.filename)
    stored_filename = f"{certificate_id}_v{version}_{safe_name}"
    file_path = UPLOAD_DIR / stored_filename
    uploaded_file.save(file_path)

    certificate_hash = file_hash(file_path)
    duplicate_id = find_duplicate(certificate_hash)
    cid = make_cid(certificate_hash)
    shard_id, shard_key, shard_reason = allocate_shard(certificate_id, issuer_name, course_name, database)

    qr_filename = f"{certificate_id}.png"
    qr_path = QR_DIR / qr_filename
    verify_url = url_for("verify_by_id", certificate_id=certificate_id, _external=True)
    qrcode.make(verify_url).save(qr_path)

    status = "duplicate" if duplicate_id else "valid"
    record = {
        "certificate_id": certificate_id,
        "version": version,
        "student_name": student_name,
        "course_name": course_name,
        "issue_date": issue_date,
        "issuer_name": issuer_name,
        "shard_id": shard_id,
        "shard_key": shard_key,
        "shard_reason": shard_reason,
        "filename": stored_filename,
        "certificate_hash": certificate_hash,
        "cid": cid,
        "qr_code": f"qr/{qr_filename}",
        "status": status,
        "duplicate_of": duplicate_id,
        "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "revoked_at": None,
        "revocation_reason": None,
    }

    if previous_record:
        previous_versions = previous_record.get("versions", [version_snapshot(previous_record)])
        if previous_versions[-1]["version"] != previous_record["version"]:
            previous_versions.append(version_snapshot(previous_record))
        record["versions"] = previous_versions + [version_snapshot(record)]
        add_audit("Version Created", certificate_id, f"Version {version} issued. Previous version: {version - 1}")
    else:
        record["versions"] = [version_snapshot(record)]

    database[certificate_id] = record
    write_json(DB_FILE, database)
    block = create_block(record)

    if duplicate_id:
        add_audit("Fraud Alert", certificate_id, f"Duplicate certificate detected. Original ID: {duplicate_id}")
    else:
        add_audit("Issued", certificate_id, f"Certificate version {version} issued and stored successfully.")
    add_audit("Shard Allocation", certificate_id, f"{certificate_id} assigned to {shard_id} using {shard_reason}.")

    return render_template("issued_result.html", record=record, block=block)


@app.route("/verify", methods=["GET", "POST"])
def verify():
    if request.method == "GET":
        return render_template("verify.html")
    certificate_id = request.form.get("certificate_id", "").strip()
    return redirect(url_for("verify_by_id", certificate_id=certificate_id))


@app.route("/verify/<certificate_id>")
def verify_by_id(certificate_id):
    database = {key: normalize_record(value) for key, value in read_json(DB_FILE, {}).items()}
    record = database.get(certificate_id)
    if not record:
        add_audit("Verification Failed", certificate_id, "Certificate ID not found.")
        return render_template("verify_result.html", status="invalid", certificate_id=certificate_id)

    file_path = UPLOAD_DIR / record["filename"]
    if not file_path.exists():
        add_audit("Verification Failed", certificate_id, "Certificate file missing from storage.")
        return render_template("verify_result.html", status="invalid", record=record)

    current_hash = file_hash(file_path)
    hash_matches = current_hash == record["certificate_hash"]
    if record["status"] == "revoked":
        status = "revoked"
    elif record["status"] == "duplicate":
        status = "duplicate"
    elif hash_matches:
        status = "valid"
    else:
        status = "tampered"

    add_audit("Verified", certificate_id, f"Verification completed with status: {status}")
    return render_template("verify_result.html", status=status, record=record, current_hash=current_hash)


@app.route("/revoke/<certificate_id>", methods=["POST"])
def revoke(certificate_id):
    database = {key: normalize_record(value) for key, value in read_json(DB_FILE, {}).items()}
    if certificate_id in database:
        reason = request.form.get("reason", "Revoked by institution").strip() or "Revoked by institution"
        database[certificate_id]["status"] = "revoked"
        database[certificate_id]["revoked_at"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        database[certificate_id]["revocation_reason"] = reason
        database[certificate_id]["versions"][-1]["status"] = "revoked"
        write_json(DB_FILE, database)
        revoke_record = dict(database[certificate_id])
        revoke_record["event"] = "REVOKE"
        create_block(revoke_record)
        add_audit("Revoked", certificate_id, f"Certificate was revoked. Reason: {reason}")
    return redirect(url_for("index"))


@app.route("/audit")
def audit_logs():
    logs = read_json(AUDIT_FILE, [])
    return render_template("audit_logs.html", logs=reversed(logs))


@app.route("/blockchain")
def blockchain():
    chain = read_json(BLOCKCHAIN_FILE, [])
    return render_template("blockchain.html", chain=chain)


@app.route("/shards")
def shards():
    database = {key: normalize_record(value) for key, value in read_json(DB_FILE, {}).items()}
    loads = shard_loads(database)
    grouped = {shard: [] for shard in SHARDS}
    for record in database.values():
        grouped.setdefault(record["shard_id"], []).append(record)
    return render_template("shards.html", grouped=grouped, loads=loads)


@app.route("/algorithms")
def algorithms():
    return render_template("algorithms.html")


if __name__ == "__main__":
    app.run(debug=True)
