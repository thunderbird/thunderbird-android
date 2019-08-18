package com.fsck.k9.backend.eas

data class ProvisionPolicyDoc(
        @field:Tag(Tags.PROVISION_DEVICE_PASSWORD_ENABLED, index = 0) val devicePasswordEnabled: Int? = 0,
        @field:Tag(Tags.PROVISION_MIN_DEVICE_PASSWORD_LENGTH, index = 1) val devicePasswordMinLength: Int? = 0,
        @field:Tag(Tags.PROVISION_ALPHA_DEVICE_PASSWORD_ENABLED, index = 2) val deviceAlphaDevicePasswordEnabled: Int? = 0,
        @field:Tag(Tags.PROVISION_MAX_INACTIVITY_TIME_DEVICE_LOCK, index = 3) val maxScreenLockTime: Int? = 0,
        @field:Tag(Tags.PROVISION_MAX_DEVICE_PASSWORD_FAILED_ATTEMPTS, index = 4) val maxPasswordFails: Int? = 0,
        @field:Tag(Tags.PROVISION_ATTACHMENTS_ENABLED, index = 5) val attachmentsEnabled: Int? = 0
)

data class ProvisionPolicyData(@field:Tag(Tags.PROVISION_EAS_PROVISION_DOC) val doc: ProvisionPolicyDoc)
data class ProvisionPolicy(
        @field:Tag(Tags.PROVISION_POLICY_TYPE, index = 0) val policyType: String,
        @field:Tag(Tags.PROVISION_POLICY_KEY, index = 1) val policyKey: String? = null,
        @field:Tag(Tags.PROVISION_STATUS, index = 2) val policyStatus: Int? = null,
        @field:Tag(Tags.PROVISION_DATA, index = 3) val policyData: ProvisionPolicyData? = null
)

data class ProvisionPolicies(@field:Tag(Tags.PROVISION_POLICY) val policy: ProvisionPolicy)
data class Provision(
        @field:Tag(Tags.PROVISION_POLICIES, index = 0) val policies: ProvisionPolicies? = null,
        @field:Tag(Tags.PROVISION_STATUS, index = 1) val status: Int? = null,
        @field:Tag(Tags.PROVISION_POLICY_KEY, index = 2) val policyKey: String? = null,
        @field:Tag(Tags.PROVISION_REMOTE_WIPE, index = 3) val remoteWipe: Int? = null
)

data class ProvisionDTO(@field:Tag(Tags.PROVISION_PROVISION) val provision: Provision)

data class FolderChange(
        @field:Tag(Tags.FOLDER_DISPLAY_NAME, index = 0) val name: String,
        @field:Tag(Tags.FOLDER_TYPE, index = 1) val folderType: Int,
        @field:Tag(Tags.FOLDER_PARENT_ID, index = 2) val parentID: Int? = null,
        @field:Tag(Tags.FOLDER_SERVER_ID, index = 3) val serverID: String
)

data class FolderChanges(
        @field:Tag(Tags.FOLDER_ADD, index = 0) val folderAdd: List<FolderChange>? = null,
        @field:Tag(Tags.FOLDER_DELETE, index = 1) val folderDelete: List<FolderChange>? = null,
        @field:Tag(Tags.FOLDER_UPDATE, index = 2) val folderUpdate: List<FolderChange>? = null,
        @field:Tag(Tags.FOLDER_COUNT, index = 3) val folderCount: Int
)

data class FolderSync(
        @field:Tag(Tags.FOLDER_SYNC_KEY, index = 0) val syncKey: String,
        @field:Tag(Tags.FOLDER_STATUS, index = 1) val status: Int? = null,
        @field:Tag(Tags.FOLDER_CHANGES, index = 2) val folderChanges: FolderChanges? = null
)

data class FolderSyncDTO(@field:Tag(Tags.FOLDER_FOLDER_SYNC) val folderSync: FolderSync?)

data class Body(
        @field:Tag(Tags.BASE_TYPE, index = 0) val type: String? = null,
        @field:Tag(Tags.BASE_DATA, index = 1) val data: String? = null,
        @field:Tag(Tags.BASE_TRUNCATED, index = 2) val truncated: Int? = null
)

data class SyncData(
        @field:Tag(Tags.EMAIL_TO, index = 0) val emailTo: String? = null,
        @field:Tag(Tags.EMAIL_FROM, index = 1) val emailFrom: String? = null,
        @field:Tag(Tags.EMAIL_CC, index = 2) val emailCC: String? = null,
        @field:Tag(Tags.EMAIL_REPLY_TO, index = 3) val emailReplyTo: String? = null,
        @field:Tag(Tags.EMAIL_DATE_RECEIVED, index = 4) val emailDateReceived: String? = null,
        @field:Tag(Tags.EMAIL_SUBJECT, index = 5) val emailSubject: String? = null,
        @field:Tag(Tags.EMAIL_READ, index = 6) val emailRead: Int? = null,
        @field:Tag(Tags.EMAIL_BODY, index = 7) val emailBody: String? = null,
        @field:Tag(Tags.BASE_BODY, index = 8) val body: Body? = null
)

data class SyncItem(
        @field:Tag(Tags.SYNC_SERVER_ID, index = 0) val serverId: String,
        @field:Tag(Tags.SYNC_APPLICATION_DATA, index = 1) val data: SyncData? = null
)

data class SyncCommands(
        @field:Tag(Tags.SYNC_ADD, index = 0) val add: List<SyncItem>? = null,
        @field:Tag(Tags.SYNC_DELETE, index = 1) val delete: List<SyncItem>? = null,
        @field:Tag(Tags.SYNC_CHANGE, index = 2) val change: List<SyncItem>? = null,
        @field:Tag(Tags.SYNC_FETCH, index = 3) val fetch: List<SyncItem>? = null
)

data class SyncResponses(
        @field:Tag(Tags.SYNC_ADD, index = 0) val add: List<SyncItem>?,
        @field:Tag(Tags.SYNC_DELETE, index = 1) val delete: List<SyncItem>?,
        @field:Tag(Tags.SYNC_CHANGE, index = 2) val change: List<SyncItem>?,
        @field:Tag(Tags.SYNC_FETCH, index = 3) val fetch: List<SyncItem>?
)

data class SyncBodyPreference(
        @field:Tag(Tags.BASE_TYPE, index = 0) val baseType: Int? = null,
        @field:Tag(Tags.BASE_TRUNCATION_SIZE, index = 1) val baseTruncationSize: Int? = null

)

data class SyncOptions(
        @field:Tag(Tags.SYNC_MIME_SUPPORT, index = 0) val mimeSupport: Int? = null,
        @field:Tag(Tags.BASE_BODY_PREFERENCE, index = 1) val bodyPreference: SyncBodyPreference? = null,
        @field:Tag(Tags.SYNC_FILTER_TYPE, index = 2) val filterType: Int? = null
)

data class SyncCollection(
        @field:Tag(Tags.SYNC_CLASS, index = 0) val clazz: String? = null,
        @field:Tag(Tags.SYNC_SYNC_KEY, index = 1) val syncKey: String? = null,
        @field:Tag(Tags.SYNC_COLLECTION_ID, index = 2) val collectionId: String? = null,
        @field:Tag(Tags.SYNC_DELETES_AS_MOVES, index = 3) val deleteAsMoves: Int? = null,
        @field:Tag(Tags.SYNC_STATUS, index = 4) val status: Int? = null,
        @field:Tag(Tags.SYNC_GET_CHANGES, index = 5) val getChanges: Int? = null,
        @field:Tag(Tags.SYNC_WINDOW_SIZE, index = 6) val windowSize: Int? = null,
        @field:Tag(Tags.SYNC_OPTIONS, index = 7) val options: SyncOptions? = null,
        @field:Tag(Tags.SYNC_COMMANDS, index = 8) val commands: SyncCommands? = null,
        @field:Tag(Tags.SYNC_RESPONSES, index = 9) val responses: SyncResponses? = null,
        @field:Tag(Tags.SYNC_MORE_AVAILABLE, index = 10) val moreAvailable: Boolean? = null
)

data class SyncCollections(@field:Tag(Tags.SYNC_COLLECTION) val collection: SyncCollection?)
data class Sync(
        @field:Tag(Tags.SYNC_COLLECTIONS, index = 0) val collections: SyncCollections?,
        @field:Tag(Tags.SYNC_WINDOW_SIZE, index = 1) val windowSize: Int? = null,
        @field:Tag(Tags.SYNC_STATUS, index = 2) val status: Int? = null
)

data class SyncDTO(@field:Tag(Tags.SYNC_SYNC) val sync: Sync)

data class PingFolder(
        @field:Tag(Tags.PING_CLASS, index = 0) val clazz: String?,
        @field:Tag(Tags.PING_ID, index = 1) val id: String?
)

data class PingFolders(@field:Tag(Tags.PING_FOLDER) val folder: List<PingFolder>?)
data class Ping(
        @field:Tag(Tags.PING_HEARTBEAT_INTERVAL, index = 0) val heartbeatInterval: Int? = null,
        @field:Tag(Tags.PING_FOLDERS, index = 1) val pingFolders: PingFolders? = null
)

data class PingDTO(@field:Tag(Tags.PING_PING) val ping: Ping)

data class PingResponseFolders(@field:Tag(Tags.PING_FOLDER) val folderId: List<String>)
data class PingResponse(
        @field:Tag(Tags.PING_FOLDERS, index = 0) val pingFolders: PingResponseFolders? = null,
        @field:Tag(Tags.PING_STATUS, index = 1) val status: Int
)

data class PingResponseDTO(@field:Tag(Tags.PING_PING) val ping: PingResponse)
