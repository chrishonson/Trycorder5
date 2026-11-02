# Pixel 9 Pro device test: composeApp

Build e71a8e8 (`claude/loving-archimedes-avhuun`), `:composeApp:assembleDebug`, installed with `adb install -r` on a Pixel 9 Pro (Android 16, 960x2142), tested 2026-11-01. The UI was driven with `adb shell input` using coordinates from uiautomator dumps. Evidence comes from screenshots, logcat, `dumpsys audio`, `dumpsys appops` and `dumpsys media.camera`.

No crash during the run: logcat has no FATAL or AndroidRuntime entries and the crash buffer is empty.

## Results

| Item | Result | Evidence |
|---|---|---|
| A1 Permissions | Pass | Camera, location, mic and notification prompts shown and granted (GrantPermissions log lines) |
| A2 Main screen | Pass | Screenshot of the main LCARS screen |
| A3 Sensors and sound | Pass | Magn, Orient, Grav, Temp pages draw. Sound toggles the MediaPlayer between started and stopped, Stop stops it |
| A4 Map | Pass | The `geo:` VIEW intent opened Maps at the current location |
| A5 Voice command | Pass | Top-left corner started the recognizer (en-US). "computer phaser" switched to FIRE, played the phaser and showed "Send: computer phaser" |
| A6 Fire, shields, transporter, tractor, motor | Pass | Shields, phaser, torpedo, transporter animation and sliders, impulse and warp GIFs, tractor push and pull, fire sliders |
| A7 Viewer | Partial | Back camera (ID 0) and front camera (ID 1) opened. Snap photo and face circles user-reported, no file found in Pictures/Trycorder |
| A8 System camera photo and record | User-reported | No new file in DCIM/Camera |
| A9 Info and logs | Pass with bug F2 | Info shows the Wi-Fi IP, matching wlan0 |
| A10 Comm, talk server, chat | Pass | Audio waveform shown, ongoing "Talk Server running on <ip>:1701" notification, chat message received |
| A11 Galleries and Vision | Partial | Video Gal., MultiSensor, Client Mode and Desktop open, Back works. Photo Gal. media prompt was denied during the run. Vision Cam1/Cam2 previews near-black with frames flowing, likely the back camera facing the desk |
| A12 Crew | Blocked on save | Contacts permission, list and admin page work. Transaction save not run because it overwrites a real contact's note (F6) |
| A13 Inventory persistence | Pass | FILL, edit, SAVE and ADD survived a force-stop and relaunch |
| A14 Settings persistence | Pass | "Speak more often" stayed checked after restart |
| A15 State restore | Pass | TRACTOR page and sound-off state restored after going to the home screen |

## Findings

| Code | Finding | Root cause | Proposed fix |
|---|---|---|---|
| F1 | Button labels clip or wrap | `ui/TrekButton.kt:35` sets 18sp text, `trycorder/TrycorderGroups.kt:19` sets 40dp buttons and `:34` allows two lines. The original layout set no text size | About 13sp, or auto-size the text |
| F2 | LOGS keeps the previous group's panel (for example the tractor controls) | `trycorder/TrycorderStore.kt:187` `GROUP_PAGES` has no `ButtonGroup.Logs` entry | Map Logs to the logs console page |
| F3 | LAT/LON/ORI text clipped on the Orient page | `sensors/ui/ReadoutPanels.kt:67` places text at `center * 2f`, which is wrong when the panel is wider than tall | Place text at the panel width minus the measured text width |
| F4 | "Trycorder Service Destroyed/Started" toasts on every page transition | Service lifecycle toasts | Drop the toasts or limit them to real start and stop |
| F5 | Setup checkboxes expose "checked" only as a content description, never a checked state | Checkbox semantics | Use toggleable semantics |
| F6 | Saving a crew transaction overwrites the contact's Note field, including Google-synced contacts | `androidMain/.../crew/ContactQueries.kt:75` `writeNote` updates or inserts `Note.NOTE` | Confirm before writing, or store transactions outside the contact |

## Visual differences from the original

- Top status line is blank where the original shows "welcome".
- Slider thumbs are round blue Material 3 thumbs instead of red bars.
- The upper viewer is black on the LOGS and TRACTOR pages.
- COMM and chat buttons clip their labels ("Dem").
- Voice listening gives no cue and recognizer errors are dropped (`AndroidSpeechListener.kt`, `onError = Unit`), as in the original. The recognizer listens for about 5 seconds after the tap.

## Not covered

A7 Snap file, A8 capture files and A11 Photo Gal. need a rerun with file and log checks. A12 save needs a test contact.
