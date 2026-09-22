# Native diagnostic inventory

Dev.31 adds declarative inventory to M1/M3 without qualifying new playback paths.
Codec roles are separate: API16/21 list decoders and encoders, with up to eight
MIME/profile/level tuples per codec. API29 supplies the manufacturer's hardware or
software classification; earlier/ambiguous declarations remain UNKNOWN. No name
heuristic manufactures hardware acceleration. This does not measure performance.

API17 reports logical display metrics and presentation membership. API23 reports
active and supported mode dimensions/refresh, which can include synthetic modes.
These declarations do not certify a physical HDMI output, cable, 4K playback or
an encoder's sustained throughput. Older APIs retain empty optional inventories.
Modern APIs are in separate classes loaded through guarded factories, with lower
API fallback after unavailable/vendor-failing inventory. No native library is added.

The Client exposes a scrollable EN/ES inventory preview under Diagnostics. Codec
preview is limited to 16 entries per role; export retains the bounded full report.
The wire allows 128 decoders, 32 encoders, eight displays and 16 modes per display.
Reports exceeding 60 KiB drop optional encoder/profile detail first, then trailing
decoders, and mark inventoryLimited. The server still enforces the 64-KiB body cap,
unique mode IDs, active-mode consistency and MIME/profile validity. Encoder records
cannot advertise playback probe candidates. Names of displays are never collected.

Existing advancing-playback evidence, expiry and planner policy still determine
playback. Inventory uploads do not create PASS results; native DIAL, CEC, RTSP,
root capture, measured acceleration and physical acceptance remain open.

Primary API references:

- [DisplayManager presentation category (API17)](https://developer.android.com/reference/android/hardware/display/DisplayManager#DISPLAY_CATEGORY_PRESENTATION)
- [Display modes and logical displays](https://developer.android.com/reference/android/view/Display)
- [Codec profiles and API29 implementation declarations](https://developer.android.com/reference/android/media/MediaCodecInfo)
