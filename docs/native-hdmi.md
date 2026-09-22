# Optional native HDMI display control

Advanced → HDMI-CEC display control offers a read-only TV power query and an
explicit Activate TV input action. The latter can wake the television and switch
its input. No operation runs on app startup or from incoming companion commands.

The adapter calls the AOSP `HdmiControlManager.getPlaybackClient()` contract,
`queryDisplayStatus()` and `oneTouchPlay()` through isolated string reflection.
It requires API21+, granted `android.permission.HDMI_CEC`, an accessible manager
and an actual playback client. The manifest declares this optional system permission;
it is not a runtime permission dialog and ordinary installations will not receive it.
Reflection never bypasses Android permission, hidden-API or SELinux enforcement.
There is no root invocation, firmware modification, JNI or manufacturer switch.

The native callback confirms the individual operation; discovery alone does not.
Queries with unknown power status remain unknown. Errors and eight-second timeouts
are bounded and two failures produce a five-minute cooldown, keyed to firmware/app
revision. Dismissal, disable and timeout fence late results and queued operations.
An already-issued native command cannot be recalled by closing the dialog.
The Advanced preference persists; Disable suppresses this optional integration.
Playback, D-pad and system-routed media keys remain independent.

Five JVM policy tests cover permission absence, explicit action, callback evidence,
late replies, cooldown and queued-command cancellation. Build/DEX checks do not
verify television behavior. Vendor-specific CEC, standby/volume/routing commands,
automatic playback integration and OEM DIAL receiver registration remain open.

The supplied Vizio backup lists `com.google.tv.dial.launcher`, but contains no
callable registration contract or extracted package manifest. That is an inventory
hint, not permission to invent an Intent or claim native YouTube integration.
The gateway YouTube TV Code/DIAL worker remains the implemented receiver path.

References (read, no source copied):

- [AOSP HDMI-CEC service and privileged access](https://source.android.com/docs/devices/tv/hdmi-cec)
- [Android 5 playback-client contract](https://android.googlesource.com/platform/frameworks/base/+/android-5.0.0_r1/core/java/android/hardware/hdmi/HdmiPlaybackClient.java)
