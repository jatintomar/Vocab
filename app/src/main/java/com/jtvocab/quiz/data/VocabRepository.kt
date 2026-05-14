package com.jtvocab.quiz.data
import android.content.Context
import com.google.gson.Gson
import com.jtvocab.quiz.model.VocabItem
import java.io.InputStreamReader

object VocabRepository {
    var ows: List<VocabItem> = emptyList()
    var synonyms: List<VocabItem> = emptyList()
    var idioms: List<VocabItem> = emptyList()
    var phrasal: List<VocabItem> = emptyList()

    fun init(context: Context) {
        try {
            val inputStream = context.assets.open("data.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val data = gson.fromJson(reader, VocabData::class.java)
            
            ows = data.ow.map { VocabItem(it.id, it.w, it.a, it.h, "ow") }
            synonyms = data.sy.map { VocabItem(it.id, it.w, it.a, it.h, "sy") }
            idioms = data.id.map { VocabItem(it.id, it.w, it.a, it.h, "id") }
            phrasal = data.pv.map { VocabItem(it.id, it.w, it.a, it.h, "ph") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class VocabData(
        val ow: List<JsonItem>,
        val sy: List<JsonItem>,
        val id: List<JsonItem>,
        val pv: List<JsonItem>
    )

    data class JsonItem(
        val id: String,
        val w: String,
        val a: String,
        val h: String
    )

    fun getItemsForSet(cat: String, setIndex: Int, size: Int): List<VocabItem> {
        val list = when(cat) {
            "ow" -> ows
            "sy" -> synonyms
            "id" -> idioms
            "ph" -> phrasal
            else -> emptyList()
        }
        val start = setIndex * size
        val end = minOf(start + size, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }
}
