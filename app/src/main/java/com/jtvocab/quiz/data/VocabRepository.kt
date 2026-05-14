package com.jtvocab.quiz.data
import com.jtvocab.quiz.model.VocabItem

object VocabRepository {
    val ows = listOf(
        VocabItem("ow1", "A person who believes that God does not exist", "Atheist", "नास्तिक", "ow"),
        VocabItem("ow2", "A collection of historical documents or records", "Archives", "पुरालेख", "ow"),
        VocabItem("ow3", "Living both on land and in water", "Amphibian", "उभयचर", "ow")
    )
    
    val synonyms = listOf(
        VocabItem("sy1", "ABANDON", "Forsake", "त्यागना", "sy"),
        VocabItem("sy2", "BENEVOLENT", "Kind", "परोपकारी", "sy")
    )
    
    val idioms = listOf(
        VocabItem("id1", "A piece of cake", "Something very easy", "बहुत आसान", "id"),
        VocabItem("id2", "Under the weather", "Feeling sick", "तबीयत ठीक न होना", "id")
    )

    fun getItemsForSet(cat: String, setIndex: Int, size: Int): List<VocabItem> {
        val list = when(cat) {
            "ow" -> ows
            "sy" -> synonyms
            else -> idioms
        }
        val start = setIndex * size
        val end = minOf(start + size, list.size)
        if (start >= list.size) return emptyList()
        return list.subList(start, end)
    }
}
