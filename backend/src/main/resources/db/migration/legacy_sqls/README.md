# Pre-squash Flyway chain (V1-V26)

This directory preserves the legacy migrations that were replaced by the greenfield V1-V14 chain.
It intentionally sits outside `classpath:db/migration`, because Flyway scans that configured location recursively.

The legacy V1 and V2 files are unchanged apart from the V1 `void` to `void_` enum correction and remain available in git history; the replaced V3-V26 files are preserved here verbatim.
