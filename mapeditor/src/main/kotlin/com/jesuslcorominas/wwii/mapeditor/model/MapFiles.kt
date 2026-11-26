package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.json.MapJson
import java.io.File

/**
 * Map files on disk. The text always goes through `MapJson`; the editor never writes the format by
 * itself. Maps are saved pretty-printed (they are reviewed in diffs) with a final newline.
 */
object MapFiles {
    /** Throws `InvalidMapException` only if the file is not a map; rule violations load fine. */
    fun read(file: File): MapDraft = MapJson.decodeDraft(file.readText())

    fun text(draft: MapDraft): String = MapJson.encode(draft, prettyPrint = true) + "\n"

    fun write(
        file: File,
        draft: MapDraft,
    ) {
        file.writeText(text(draft))
    }
}
