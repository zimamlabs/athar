# Security

Athar keeps everything in plain files on your own device and makes no network calls.

If you find a way to read, change or lose someone's data, or a way for the clipboard history to record something it should not, please report it privately through GitHub's "Report a vulnerability" on the Security tab of this repository rather than in a public issue. Include the version, the system and the steps. You will get an answer within a few days.

The clipboard history is a privacy-sensitive feature: it is off until turned on, never reads the clipboard while off, skips texts matching the user's patterns, and stores only in `clipboard/history.json` in the user's folder.
