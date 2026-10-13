# Thunderbird Graph API project

This folder documents the additive Microsoft Graph REST backend implemented under `backend/graph` and wired to the separate `graph` backend factory. The FairEmail checkout at `/workspace/fairemail` was used only as a read-only behavior reference; this Thunderbird work does not copy its OAuth identifiers or redirect URIs.

- [Graph API design and integration boundary](GRAPH_API_DESIGN.md)
- [Implementation notes and remaining onboarding prerequisite](IMPLEMENTATION_NOTES.md)

The backend is **polling-only** and supports Graph send, Move/Copy, read/star changes, and deletion. Arbitrary message upload/draft append and server-side search remain unsupported; existing IMAP/POP paths and Microsoft IMAP/SMTP OAuth scopes remain unchanged. Send timeouts without a response are treated as outcome unknown and are not automatically retried. No remote branch or upstream repository was modified.
