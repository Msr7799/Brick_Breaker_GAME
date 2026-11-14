package com.example.brick_breaker_ball.campaign

import com.example.brick_breaker_ball.app.CloudProgressRecord
import com.example.brick_breaker_ball.app.CloudProgressResult
import com.example.brick_breaker_ball.app.PlatformActions
import java.util.UUID

/** Rendering-thread coordinator: read before upload, CAS writes, durable offline copies. */
internal class CloudProgressSync(
    private val platform: PlatformActions,
    private val reloaded: () -> Unit,
    private val ready: () -> Unit
) {
    var message = "Connecting cloud save..."
        private set
    var conflict: CloudProgressRecord? = null
        private set
    var initialComplete = !platform.cloudProgressEnabled
        private set
    private var account: String? = null
    private var generation = 0
    private var busy = false
    private var readComplete = false
    private var nextCheck = 0L
    private var nextRemoteCheck = 0L
    private var queued: CloudProgressRecord? = null
    private var canApply = false
    val accountReady: Boolean get() = readComplete && conflict == null && account == platform.dailyRewardAccountId
    private fun key(suffix: String) = "${CloudSaveCodec.hash(requireNotNull(account))}_$suffix"
    private fun revision() = CloudSaveCodec.meta().getLong(key("revision"), 0L)
    private fun baseline() = CloudSaveCodec.meta().getString(key("baseline"), "")
    private fun bound() = CloudSaveCodec.meta().getString("bound_account", "")

    fun retry() { nextCheck = 0L; nextRemoteCheck = 0L; if (!busy) readComplete = false }
    fun tick(safeToRestore: Boolean, force: Boolean = false) {
        canApply = safeToRestore
        if (!platform.cloudProgressEnabled) return
        val uid = platform.dailyRewardAccountId
        if (uid != account) {
            generation++
            account = uid
            busy = false; readComplete = false; conflict = null; queued = null
            nextCheck = 0L; nextRemoteCheck = 0L
        }
        if (uid == null) { message = "Offline - connect Play Games to back up progress"; return }
        if (queued != null && safeToRestore) {
            val record = requireNotNull(queued)
            queued = null
            acceptRead(record)
        }
        if (queued != null) return
        if (busy || conflict != null) return
        val now = System.nanoTime()
        if (!force && now < nextCheck) return
        nextCheck = now + 3_000_000_000L
        if (!readComplete || now >= nextRemoteCheck) { read(); return }
        if (bound() != uid) return
        val snapshot = CloudSaveCodec.capture()
        if (CloudSaveCodec.hash(snapshot) != baseline()) upload(snapshot)
    }
    private fun read() {
        val uid = account ?: return
        val requestGeneration = generation
        busy = true
        message = "Checking your cloud save..."
        platform.readCloudProgress(uid) { result ->
            if (requestGeneration != generation || platform.dailyRewardAccountId != uid) return@readCloudProgress
            busy = false
            val record = result.record
            if (record == null) failed(result.error)
            else if (!canApply) { queued = record; message = "Cloud save ready - return to the main menu" }
            else runCatching { acceptRead(record) }.onFailure { failed("Cloud save could not be read. Local progress is safe.") }
        }
    }
    private fun candidate(): String {
        val uid = requireNotNull(account)
        return if (bound().isBlank() || bound() == uid) CloudSaveCodec.capture()
        else CloudSaveCodec.meta().getString(key("backup"), "").ifBlank { CloudSaveCodec.empty() }
    }
    private fun acceptRead(raw: CloudProgressRecord) {
        require(raw.revision >= 0 && ((raw.snapshot == null) == (raw.revision == 0L)))
        val remote = raw.snapshot?.let(CloudSaveCodec::canonical)
        val record = raw.copy(snapshot = remote)
        val local = candidate()
        val localHash = CloudSaveCodec.hash(local)
        val metadata = CloudSaveCodec.meta()
        // Recover an acknowledged write whose HTTP response was lost. Preserve later local edits.
        if (remote != null && record.saveId == metadata.getString(key("pending_id"), "")) {
            val sent = metadata.getString(key("pending_snapshot"), "")
            if (sent.isNotBlank() && CloudSaveCodec.hash(CloudSaveCodec.canonical(sent)) == CloudSaveCodec.hash(remote)) {
                if (bound() != account) install(local)
                acknowledge(record.revision, CloudSaveCodec.hash(remote))
                finishRead()
                return
            }
        }
        if (remote == null) {
            if (bound() != account) install(local)
            metadata.putLong(key("revision"), 0L).putString(key("baseline"), "").flush()
            finishRead()
            upload(CloudSaveCodec.capture())
        } else if (localHash == CloudSaveCodec.hash(remote)) {
            if (bound() != account) install(local)
            acknowledge(record.revision, localHash)
            finishRead()
        } else if (record.revision == revision() && baseline().isNotBlank()) {
            if (bound() != account) install(local)
            finishRead() // Unsynced offline edits are newer than this acknowledged revision.
        } else if (!CloudSaveCodec.meaningful(local) || (baseline().isNotBlank() && localHash == baseline())) {
            install(remote)
            acknowledge(record.revision, CloudSaveCodec.hash(CloudSaveCodec.capture()))
            finishRead()
            message = "Cloud progress restored"
        } else {
            if (bound() != account) install(local)
            conflict = record
            message = "Two saves found. Choose the progress to keep."
        }
    }
    private fun install(snapshot: String) {
        check(canApply)
        val uid = requireNotNull(account)
        val previous = bound()
        if (previous.isNotBlank()) CloudSaveCodec.meta()
            .putString("${CloudSaveCodec.hash(previous)}_backup", CloudSaveCodec.capture()).flush()
        CloudSaveCodec.apply(snapshot, uid)
        reloaded()
    }
    private fun finishRead() {
        readComplete = true
        initialComplete = true
        nextRemoteCheck = System.nanoTime() + 60_000_000_000L
        message = "Cloud save connected"
        ready()
    }
    private fun acknowledge(version: Long, hash: String) {
        val metadata = CloudSaveCodec.meta()
        metadata.putLong(key("revision"), version).putString(key("baseline"), hash)
        metadata.remove(key("pending_id")); metadata.remove(key("pending_snapshot")); metadata.flush()
    }
    private fun upload(snapshot: String) {
        val uid = account ?: return
        val requestGeneration = generation
        val id = UUID.randomUUID().toString().replace("-", "")
        CloudSaveCodec.meta().putString(key("pending_id"), id).putString(key("pending_snapshot"), snapshot).flush()
        busy = true
        message = "Saving progress to cloud..."
        platform.writeCloudProgress(uid, revision(), id, snapshot) { result ->
            if (requestGeneration != generation || platform.dailyRewardAccountId != uid) return@writeCloudProgress
            busy = false
            if (result.conflict && result.record != null) {
                // Never combine consumable balances or silently overwrite another device.
                val remote = result.record
                val checkpoint = remote.snapshot?.let(CloudSaveCodec::canonical)
                if (checkpoint != null && CloudSaveCodec.hash(checkpoint) == CloudSaveCodec.hash(snapshot)) {
                    acknowledge(remote.revision, CloudSaveCodec.hash(snapshot))
                    message = "Progress saved to cloud"
                } else {
                    if (checkpoint != null) conflict = remote.copy(snapshot = checkpoint) else readComplete = false
                    message = "Two saves found. Choose the progress to keep."
                }
            } else if (result.record != null && result.record.saveId == id) {
                acknowledge(result.record.revision, CloudSaveCodec.hash(snapshot))
                message = "Progress saved to cloud"
            } else failed(result.error)
        }
    }
    private fun failed(error: String?) {
        readComplete = false // Read again before retrying a possibly committed write.
        nextCheck = System.nanoTime() + 15_000_000_000L
        message = error ?: "Offline - progress is saved on this device"
    }
    fun chooseCloud() {
        if (!canApply) return
        val remote = conflict ?: return
        val snapshot = remote.snapshot ?: return
        install(snapshot)
        conflict = null
        acknowledge(remote.revision, CloudSaveCodec.hash(CloudSaveCodec.capture()))
        finishRead()
        message = "Cloud progress restored"
    }
    fun chooseDevice() {
        if (!canApply) return
        val remote = conflict ?: return
        CloudSaveCodec.meta().putString(key("rejected_cloud_backup"), remote.snapshot ?: "").flush()
        conflict = null
        CloudSaveCodec.meta().putLong(key("revision"), remote.revision).flush()
        finishRead()
        upload(CloudSaveCodec.capture())
    }
    fun localSummary(): String = CloudSaveCodec.summary(candidate())
    fun cloudSummary(): String = conflict?.snapshot?.let(CloudSaveCodec::summary) ?: "No cloud save"
}
