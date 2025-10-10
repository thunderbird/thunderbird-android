# Demo Backend

This module provides a self‑contained, offline backend implementation used by the app to showcase and manually test
email UI and flows without connecting to a real mail server. It implements the Backend API and loads demo data from
resources bundled with the library.

## What it does

- Exposes a Backend that
  - returns a predefined folder list
  - supports basic sync of message lists
  - supports threaded conversations (based on standard Message-Id, In-Reply-To, and References headers)
  - pretends to move/copy/upload messages successfully
  - sends messages by handing them to the app storage layer (no network)
- Loads folders and messages from `src/main/resources/mailbox`.

## How data is organized

- Folder tree definition: `src/main/resources/mailbox/contents.json`
  - Describes folders by `serverId`, display name, type (INBOX, SENT, …), and a list of `messageServerIds` per folder.
  - Supports nested folders through the `subFolders` field. The backend flattens nested folders internally so they show up as "Parent/Child" names.
  - Special folders (Inbox, Drafts, Sent, Spam, Trash, Archive) are ensured to always exist.
- Messages: EML files in `src/main/resources/mailbox/<folderServerId>/<messageServerId>.eml`
  - Example: `src/main/resources/mailbox/inbox/intro.eml` corresponds to `folderServerId=inbox` and `messageServerId=intro`.

Key classes

- DemoBackend: Backend implementation wired to simple commands.
- DemoStore: In‑memory source of truth backed by resources.
- DemoDataLoader: Reads contents.json and parses .eml files into Message objects.

Limitations (by design)

- No real network access; search, part fetching, and some operations are not implemented and will throw.
- Push is not supported.
- Only data found in contents.json and the matching .eml files is available.

## Using the demo backend in apps

This module is a Kotlin/JVM library. Applications can depend on `backend:demo` and select the demo backend when creating
accounts for testing/development. The exact wiring is app‑specific (see the app modules in this repository for how
they register/select backends).

## Editing demo content

1) Add a new message
- Place your EML file at: `src/main/resources/mailbox/<folderServerId>/<yourMessageId>.eml`
- Run `./gradlew :backend:demo:updateDemoMailbox` to regenerate `src/main/resources/mailbox/contents.json`.

2) Add a new folder (optionally nested)
- Create the corresponding directory under `src/main/resources/mailbox/<yourFolderServerId>/` and place the `.eml` files inside it.
- Then run `./gradlew :backend:demo:updateDemoMailbox` to update `contents.json`.

### Threaded messages

Threading is supported by the app when messages include standard headers. The demo backend simply exposes the messages; the UI groups them into threads.

To create a conversation thread in a folder:

- Give each message a unique `Message-Id` header.
- For replies, set `In-Reply-To` to the `Message-Id` of the parent message.
- Maintain a `References` header that contains the chain of ancestor `Message-Id`s (root first, then each reply). Many clients do this automatically; for demo EMLs, edit the headers manually.
- Place all messages of a thread in the same folder.
- Subject prefixes like `Re:` are optional and not used for threading.

Example headers in a reply message:

```
Message-Id: <reply-2@example.test>
In-Reply-To: <root-1@example.test>
References: <root-1@example.test>
```

Notes and limitations for threads:

- Cross-folder threading is not supported by the demo backend; keep a thread’s messages in one folder.
- The backend does not infer threads from filenames or `messageServerIds`; only the MIME headers control threading.

