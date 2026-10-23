# Screen captures

These PNGs show the existing Compose examples rendered on an Android device. Bird selection is the default app screen; the counter, snackbar, and rotating box are separate lesson composables.

To refresh a capture, display the corresponding composable inside `KotlinCoroutinesFlowTheme` in `MainActivity`, run the app, and save a screenshot from Android Studio's Running Devices window. Restore `CoroutineBasicsHomework.Assignment3()` after capturing other lessons.

- `bird-picker.png`: initial bird selection screen.
- `bird-pigeon.png`, `bird-crow.png`, `bird-sparrow.png`: each selected bird.
- `counter.png`: the automatic counter after it has advanced.
- `profile-idle.png`: snackbar example before a tap.
- `profile-snackbar.png`: snackbar example while its message is visible.
- `rotating-box.png`: one frame of the rotating box animation.

For the visible-snackbar capture, the snackbar duration was temporarily held open so capture-tool latency would not dismiss it. The committed example retains its default duration. The rotating-box capture uses the full-size modifier shown in the root README.

A still image cannot demonstrate timing or cancellation; the walkthrough and diagrams in the root README explain those behaviors. These examples print bird sounds to Logcat; they do not play audio.
