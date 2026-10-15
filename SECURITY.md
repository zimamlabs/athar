# Security

Athar keeps your data on your own device in plain files and has no server and no network calls, so most reports are about the app itself: how files are read and written, permissions, the clipboard history and the signed downloads.

## Report a problem

Please report a security issue privately, not in a public issue: open the repository's **Security** tab and choose **Report a vulnerability** (GitHub private advisory). Say what you found, which version and system, and how to reproduce it. You will get an answer within a few days.

## What counts

- Data loss or corruption when files are moved, copied, imported or restored.
- Reading or writing files outside the app's own folder (`Zimam/Athar`).
- The clipboard history recording something it should not. It is desktop only, off by default and never synced; it never reads the clipboard while off, skips texts that match the user's patterns, and stores only `clipboard/history.json` in the user's folder.
- A download whose signature or checksum does not match the fingerprint in the README.

## Supported versions

Only the latest release gets fixes.
