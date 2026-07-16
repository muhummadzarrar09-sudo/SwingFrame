# Phase 1 device test matrix

Record phone model, Android version, RAM, and available storage before testing.

| Container / codec | Resolution | FPS/timing | Import | Indexed count | First frame | Step exact | Scrub | Playback | Notes |
|---|---:|---:|---|---:|---|---|---|---|---|
| MP4 / H.264 | 1080p | 30 CFR | | | | | | | |
| MP4 / H.264 | 1080p | 60 CFR | | | | | | | |
| MP4 / H.265 | 4K | 30 CFR | | | | | | | |
| MP4 / H.265 | 1080p | 120/240 | | | | | | | |
| MOV / H.264 or HEVC | 1080p | VFR | | | | | | | |
| WebM / VP9 | 1080p | 30 | | | | | | | |
| MKV / supported codec | 1080p | 30 | | | | | | | |
| 3GP / H.263 | phone sample | 30 | | | | | | | |
| HDR phone clip | 4K | 30/60 | | | | | | | color check |
| Deliberately truncated file | any | any | clear error | n/a | n/a | n/a | n/a | n/a | no crash |

For a 10-second 1080p/60 clip, also record:

- Metadata probe time
- Timestamp index time
- First-frame latency
- Warm adjacent-frame latency
- Cold random-seek latency
- Peak memory from Android Studio profiler
- Whether any adjacent indices display duplicate pixels unexpectedly
