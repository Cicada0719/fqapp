package com.fqapp.fqapp

import android.os.Build
import android.util.Log
import io.flutter.embedding.engine.FlutterShellArgs
import java.util.Locale

/** Startup policy for the MT6895/Dimensity 8100 video texture artifacts in #14. */
internal object RendererCompatibility {
    // Note: affected chipset scope and release flag behavior — see
    // .agents/notes/implemented/bug-fix/2026-10-15-mt6895-renderer-compatibility.md.
    private val platformId = Regex("^mt6895(?:[a-z].*)?$")
    private val productName = Regex("^(?:mediatek)?dimensity8100(?:max|ultra)?$")

    fun configure(args: FlutterShellArgs) {
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            ""
        }
        val affected = listOf(Build.HARDWARE, Build.BOARD, socModel).any { value ->
            val normalized = value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
            platformId.matches(normalized) || productName.matches(normalized)
        }
        if (!affected) return

        // FlutterShellArgs is a HashSet, so retaining contradictory values would
        // leave precedence to iteration order. Preserve every unrelated flag.
        for (arg in args.toArray()) {
            if (arg == "--enable-impeller" || arg.startsWith("--enable-impeller=") ||
                arg.startsWith("--impeller-backend=")) {
                args.remove(arg)
            }
        }
        // In 3.44.6 release, ImpellerBackend alone does not force GLES. Opting out
        // of Impeller selects Skia/OpenGL ES; it does not disable hardware decode.
        args.add(FlutterShellArgs.ARG_DISABLE_IMPELLER)
        Log.i("FqRenderer", "policy=skia-opengles reason=mt6895-or-dimensity8100")
    }
}
