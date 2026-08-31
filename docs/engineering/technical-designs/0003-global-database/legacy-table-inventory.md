# Global Database: Legacy Table Inventory

Supporting document for [Technical Design 0003: Global Database](../0003-global-database.md).

## Purpose

This inventory is the source of truth for the legacy-to-global migration mapping.
It is deliberately separate from the technical design because the table-level implementation detail will evolve while
the design's migration contract remains stable.

Every durable legacy table is copied to a legacy-compatible target representation.
`messages_fulltext` is rebuilt because it is derived data. No other table is deferred without an explicit, tested
compatibility decision.

|               Legacy source                | Classification |                                                                                                                                                  Global target and import rule                                                                                                                                                  |
|--------------------------------------------|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `account_extra_values`                     | Copy           | Preserve text/integer values and absence semantics, scoped to `AccountId`.                                                                                                                                                                                                                                                      |
| `folders`                                  | Copy           | Preserve every field. Map `(account_id, legacy_local_id)` and associate a global `FolderId` when required.                                                                                                                                                                                                                      |
| `folder_extra_values`                      | Copy           | Preserve unknown keys and values through the account-qualified folder mapping.                                                                                                                                                                                                                                                  |
| `messages`                                 | Copy           | Preserve metadata, flags, previews, encryption, and new-message state. Map to a global `MessageId` when required.                                                                                                                                                                                                               |
| `message_parts`                            | Copy           | Preserve MIME-tree relationships, metadata, `data_location`, and every `data` BLOB. Legacy stores a body part in `data` at or below the 16 KiB threshold and on disk above it, regardless of whether the part is an attachment, so in-database attachment bytes stay BLOBs.                                                     |
| `threads`                                  | Copy           | Preserve existing message, root, and parent relationships. Do not recalculate threading in this migration.                                                                                                                                                                                                                      |
| `outbox_state`                             | Copy           | Preserve send state, attempts, error timestamps, and error-state semantics.                                                                                                                                                                                                                                                     |
| `pending_commands`                         | Copy           | Preserve command and serialized payload with the source account. Preserve account-qualified local IDs so execution resolves the mapped target records without redesigning queue behavior.                                                                                                                                       |
| `messages_fulltext`                        | Rebuild        | Rebuild from imported durable content, setting `docid` to the imported message's id so search joins resolve. Coverage equals messages whose body parts are locally present, because the legacy index was populated from text supplied at save time. Validate representative account and unified search parity against that set. |
| `notifications`                            | Copy           | Preserve notification IDs and timestamps through the account-qualified message mapping.                                                                                                                                                                                                                                         |
| `<accountId>.db_att/<legacyMessagePartId>` | Copy           | Covers parts with `data_location = 2` (`ON_DISK`) only. Keep that content in the global file-backed directory. Do not move it into database BLOB storage, and do not promote in-database BLOBs to files.                                                                                                                        |

For all account-local numeric IDs, target mappings preserve `(account_id, legacy_local_id)` and record a global domain
identifier only where the repository contract requires one.

The fts4 shadow tables `messages_fulltext_content`, `_segdir`, `_segments`, `_docsize`, and `_stat` are excluded. They
are rebuilt with their virtual table and are not durable inputs.

## Schema behavior

The legacy schema carries six triggers whose effects are part of mail behavior, not performance tuning:

|           Trigger            |                                        Effect to preserve                                         |
|------------------------------|---------------------------------------------------------------------------------------------------|
| `set_message_part_root`      | Sets `message_parts.root = id` on insert when `root` is null.                                     |
| `set_thread_root`            | Sets `threads.root = id` on insert when `root` is null.                                           |
| `new_message_reset`          | Clears `messages.new_message` when `read` becomes 1.                                              |
| `delete_message`             | Deletes `message_parts` by `root`, `messages_fulltext` by `docid`, and `threads` by `message_id`. |
| `delete_folder`              | Deletes `messages` for the folder.                                                                |
| `delete_folder_extra_values` | Deletes `folder_extra_values` for the folder.                                                     |

Each effect is reproduced as a trigger, a foreign-key cascade, or repository logic. The choice is recorded here per
trigger and tested for parity. Indexes are reproduced for query-plan parity but carry no behavior.

## Implementation prerequisites

Before implementation, complete and test the column-level mapping, trigger/index compatibility checks, supported
source-schema fixtures, and representative fixtures for every source listed here. Source reading must be read-only and
must not invoke legacy repair or upgrade behavior. Queue fixtures must deserialize and execute each supported command
after restart using its account-qualified mappings.
