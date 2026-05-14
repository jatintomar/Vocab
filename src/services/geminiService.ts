import { GoogleGenAI, Type } from "@google/genai";

let aiInstance: GoogleGenAI | null = null;

function getAI() {
  if (!aiInstance) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      console.warn("GEMINI_API_KEY not found. AI features may fall back to default text.");
    }
    aiInstance = new GoogleGenAI({ apiKey });
  }
  return aiInstance;
}

export interface WordInsight {
  context: string;
  mnemonic: string;
  usage: string;
  synonyms: string[];
}

const insightCache: Record<string, WordInsight> = {};

function cleanJSON(text: string): string {
  // Remove markdown code blocks if present
  return text.replace(/```json\n?|```/g, "").trim();
}

export async function getWordInsight(word: string, category: string, force: boolean = false): Promise<WordInsight> {
  const cacheKey = `${category}:${word.toLowerCase()}`;
  if (!force && insightCache[cacheKey]) {
    return insightCache[cacheKey];
  }

  const ai = getAI();
  const prompt = `
    Analyze the following English word: "${word}" (Category: ${category}).
    Provide:
    1. "SSC Exam Context": Typical usage in exams.
    2. "Mnemonic": Memory trick.
    3. "Example Sentence": A clear, high-yield sentence.
    4. "Top 3 Synonyms": Comma separated.
    Return ONLY a valid JSON object.
  `;

  try {
    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: prompt,
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            context: { type: Type.STRING },
            mnemonic: { type: Type.STRING },
            usage: { type: Type.STRING },
            synonyms: { type: Type.ARRAY, items: { type: Type.STRING } },
          },
          required: ["context", "mnemonic", "usage", "synonyms"],
        },
      },
    });

    const result = JSON.parse(cleanJSON(response.text));
    insightCache[cacheKey] = result;
    return result;
  } catch (error) {
    console.error("Gemini API Error:", error);
    return {
      context: "Strategic context: This term frequently appears in SSC reading comprehension and fill-in-the-blanks.",
      mnemonic: "Focus on the root meaning and associate it with its synonyms for better retention.",
      usage: `Competitive success often depends on deep understanding of words like "${word}".`,
      synonyms: ["Related term", "Exam high-yield"]
    };
  }
}

export async function getDailyInsight(): Promise<{ word: string, insight: string, usage: string }> {
  const ai = getAI();
  const prompt = `
    Provide a "Daily Vocabulary Pulse" for an SSC aspirant preparing for 2026.
    Pick one very important high-yield word and provide:
    1. The word itself.
    2. A "Power Insight": Why this word is crucial specifically for competitive exams.
    3. A "Modern Usage": How it might appear in a current 2026 news context.
    Return ONLY a valid JSON object.
  `;

  try {
    const response = await ai.models.generateContent({
      model: "gemini-3-flash-preview",
      contents: prompt,
      config: {
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            word: { type: Type.STRING },
            insight: { type: Type.STRING },
            usage: { type: Type.STRING },
          },
          required: ["word", "insight", "usage"],
        },
      },
    });
    return JSON.parse(cleanJSON(response.text));
  } catch (error) {
    return {
      word: "Persistence",
      insight: "Consistency is more important than intensity in SSC prep.",
      usage: "The candidate's persistence in mastering idioms paid off in the final tier."
    };
  }
}

