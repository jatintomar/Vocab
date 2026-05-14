package com.jtvocab.quiz.data
import com.jtvocab.quiz.model.VocabItem
import com.jtvocab.quiz.model.PQRSQuestion
import com.jtvocab.quiz.model.ClozeQuestion

object VocabRepository {
    val ows = listOf(
        VocabItem("ow1", "A person who believes that God does not exist", "Atheist", "नास्तिक", "ow"),
        VocabItem("ow2", "A collection of historical documents or records", "Archives", "पुरालेख", "ow"),
        VocabItem("ow3", "Living both on land and in water", "Amphibian", "उभयचर", "ow"),
        VocabItem("ow4", "A decision on which one cannot go back", "Irrevocable", "अटल", "ow"),
        VocabItem("ow5", "A place where bees are kept", "Apiary", "मधुमक्खी शाला", "ow"),
        VocabItem("ow6", "One who feeds on human flesh", "Cannibal", "नरभक्षी", "ow"),
        VocabItem("ow7", "A collection of poems", "Anthology", "कविता संग्रह", "ow")
    )
    
    val synonyms = listOf(
        VocabItem("sy1", "ABANDON", "Forsake", "त्यागना", "sy"),
        VocabItem("sy2", "BENEVOLENT", "Kind", "परोपकारी", "sy"),
        VocabItem("sy3", "CANDID", "Frank", "स्पष्टवादी", "sy"),
        VocabItem("sy4", "DOCILE", "Submissive", "विनम्र", "sy"),
        VocabItem("sy5", "ELATED", "Joyful", "उत्साहित", "sy")
    )
    
    val idioms = listOf(
        VocabItem("id1", "A piece of cake", "Something very easy", "बहुत आसान", "id"),
        VocabItem("id2", "Under the weather", "Feeling sick", "तबीयत ठीक न होना", "id"),
        VocabItem("id3", "Spill the beans", "Reveal a secret", "भेद खोलना", "id"),
        VocabItem("id4", "Burn the midnight oil", "Work very hard", "देर रात तक काम करना", "id")
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

    val dailyPQRS = listOf(
        PQRSQuestion(
            id = "pq1",
            s1 = "S1: The rise of automation has sparked debate.",
            s6 = "S6: Ultimately, a nuanced approach is required.",
            sentences = listOf(
                "P: AI can increase efficiency universally.",
                "Q: Critics warn of job displacement.",
                "R: This tension lies at the heart of tech.",
                "S: Lack of transparency complicates trust."
            ),
            correctSequence = "PQSR",
            explanation = "Logical flow from benefits to critics to trust issues."
        ),
        PQRSQuestion(
            id = "pq2",
            s1 = "S1: Space exploration has always fascinated humanity.",
            s6 = "S6: The final frontier remains our greatest challenge.",
            sentences = listOf(
                "P: Mars has become the primary target for colonization.",
                "Q: Technological hurdles still block the path.",
                "R: Private companies are now leading the charge.",
                "S: Resources are being diverted from Earthly needs."
            ),
            correctSequence = "RPQS",
            explanation = "Discusses the shift to private companies and their goals."
        ),
        PQRSQuestion(id = "pq3", s1 = "S1: History", s6 = "S6: End", sentences = listOf("P: A", "Q: B", "R: C", "S: D"), correctSequence = "PQRS", explanation = "NA"),
        PQRSQuestion(id = "pq4", s1 = "S1: Science", s6 = "S6: End", sentences = listOf("P: A", "Q: B", "R: C", "S: D"), correctSequence = "PQRS", explanation = "NA"),
        PQRSQuestion(id = "pq5", s1 = "S1: Arts", s6 = "S6: End", sentences = listOf("P: A", "Q: B", "R: C", "S: D"), correctSequence = "PQRS", explanation = "NA")
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
