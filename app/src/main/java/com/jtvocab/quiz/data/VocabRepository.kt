package com.jtvocab.quiz.data
import android.content.Context
import com.google.gson.Gson
import com.jtvocab.quiz.model.VocabItem
import com.jtvocab.quiz.model.PQRSQuestion
import com.jtvocab.quiz.model.ClozeQuestion
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

    val dailyPQRS = listOf(
        PQRSQuestion(
            id = "pq1",
            s1 = "The expansion of the tech industry has been significant.",
            s6 = "This trend is likely to continue for the foreseeable future.",
            sentences = listOf(
                "P: Silicon Valley remains the epicenter of innovation.",
                "Q: Emerging markets are now challenging this dominance.",
                "R: Cloud computing has enabled global scalability.",
                "S: Remote work has further decentralized the workforce."
            ),
            correctSequence = "RPSQ",
            explanation = "Logical flow from infrastructure (Cloud) to center (Silicon Valley) to decentralized shifts (Emerging/Remote)."
        ),
        PQRSQuestion(
            id = "pq2",
            s1 = "Quantum mechanics and general relativity are both well-tested.",
            s6 = "The quest for a 'Theory of Everything' remains the holy grail of physics.",
            sentences = listOf(
                "P: However, they are mathematically incompatible at extreme scales.",
                "Q: This conflict arises in the study of black hole singularities.",
                "R: String theory has been proposed as a potential bridge.",
                "S: Experimental verification remains elusive despite decades of research."
            ),
            correctSequence = "PQRS",
            explanation = "Discusses the incompatibility, the specific conflict point, and the proposed (but unverified) solution."
        ),
        PQRSQuestion(
            id = "pq3",
            s1 = "The Industrial Revolution marked a major turning point in history.",
            s6 = "The era set the stage for the modern consumer-driven economy.",
            sentences = listOf(
                "P: Steam power replaced manual labor in textile mills.",
                "Q: This led to mass migration from rural areas to urban centers.",
                "R: Living conditions in these new cities were initially abysmal.",
                "S: Legislative reforms eventually mitigated the worst impacts."
            ),
            correctSequence = "PQRS",
            explanation = "Chronological sequence from technology to migration to social impact and finally reform."
        ),
        PQRSQuestion(
            id = "pq4",
            s1 = "A substantial body of literature exists on macro-economic stability.",
            s6 = "Continuous monitoring by central banks is thus indispensable.",
            sentences = listOf(
                "P: Fiscal policy and monetary mechanisms must work in tandem.",
                "Q: Inflationary pressures can destabilize even the strongest currencies.",
                "R: Market volatility often stems from unpredictable geopolitical events.",
                "S: Proactive interventions are required to maintain equilibrium."
            ),
            correctSequence = "RQPS",
            explanation = "Starts with causes of instability and moves to the required policy coordination and intervention."
        ),
        PQRSQuestion(
            id = "pq5",
            s1 = "Ecological conservation has shifted from local to global focus.",
            s6 = "The survival of biodiversity depends on this unified commitment.",
            sentences = listOf(
                "P: International treaties now dictate biodiversity standards.",
                "Q: Sovereign nations often resist these outside mandates.",
                "R: Economic development is frequently prioritized over habitat preservation.",
                "S: Yet, the shared nature of climate risks forces cooperation."
            ),
            correctSequence = "PQRS",
            explanation = "Outlines the global shift, the resistance encountered, the conflict with economy, and the ultimate necessity of cooperation."
        )
    )

    val dailyCloze = listOf(
        ClozeQuestion(
            id = "cl1",
            passage = "The structural (1) of post-colonial infrastructure often manifests as a simultaneity of obsolescence and futurity. Flyovers calcified mid-renovation do not signify simple (2) but rather a disjunctive temporality. Here, functionality is (3) to the point. What matters is the (4) illusion of development.",
            blanks = listOf(
                ClozeQuestion.Blank(1, listOf("tenacity", "attrition", "malaise", "fixation"), "attrition", "Context of decay."),
                ClozeQuestion.Blank(2, listOf("repair", "malfunction", "success", "design"), "malfunction", "Technical failure."),
                ClozeQuestion.Blank(3, listOf("beside", "central", "critical", "pivotal"), "beside", "Idiom 'beside the point'."),
                ClozeQuestion.Blank(4, listOf("vivid", "performative", "actual", "meager"), "performative", "Acting for show.")
            )
        ),
        ClozeQuestion(
            id = "cl2",
            passage = "Language is (1) more than a tool for communication. It (2) the soul of a culture. When a language (3), its unique worldview also (4).",
            blanks = listOf(
                ClozeQuestion.Blank(1, listOf("merely", "rarely", "notably", "highly"), "merely", "Contrastive focus."),
                ClozeQuestion.Blank(2, listOf("embodies", "destroys", "limits", "hides"), "embodies", "Positive relationship."),
                ClozeQuestion.Blank(3, listOf("thrives", "dies", "grows", "changes"), "dies", "Context of loss."),
                ClozeQuestion.Blank(4, listOf("expands", "vanishes", "improves", "persists"), "vanishes", "Consequence of loss.")
            )
        )
    )

    val dailyRC = com.jtvocab.quiz.model.RCQuestion(
        id = "rc1",
        passage = "The Industrial Revolution was a period of global transition of the human economy towards more widespread, efficient and stable manufacturing processes that succeeded the Agricultural Revolution. This process started in Great Britain and then spread to the rest of the world.",
        questions = listOf(
            com.jtvocab.quiz.model.RCQuestion.RCSubQuestion(
                q = "Where did the Industrial Revolution start?",
                options = listOf("USA", "Great Britain", "France", "Germany"),
                a = "Great Britain",
                explanation = "Directly stated in the passage."
            )
        )
    )
}
