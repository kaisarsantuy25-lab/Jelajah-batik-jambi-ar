package com.jelajahbatikjambi.data.model

/**
 * Metadata for one Batik Jambi motif, keyed by [markerId] (the id used by
 * the AR detector to reveal it in the scanner). Motifs come from two
 * sources: the six bundled entries in `assets/data/batik.json` (§46), and
 * custom motifs registered by the user through the Tambah Motif flow.
 *
 * [material] and [coloring] are the "Bahan Dasar" and "Jenis Pewarnaan"
 * attributes shown on the detail page; they are blank for user-added motifs
 * until the user fills them in on the edit screen.
 */
data class BatikData(
    val id: Int,
    val markerId: Int,
    val name: String,
    val category: String,
    val shortDescription: String,
    val meaning: String,
    val history: String,
    val imagePath: String?,
    val modelPath: String?,
    val material: String = "",
    val coloring: String = ""
)
