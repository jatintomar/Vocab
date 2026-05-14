import { GoogleGenAI, Type } from "@google/genai";

let aiInstance: GoogleGenAI | null = null;

function getAI() {
  if (!aiInstance) {
    const apiKey = typeof process !== 'undefined' ? process.env.GEMINI_API_KEY : undefined;
    if (!apiKey) {
      console.warn("GEMINI_API_KEY not found in process.env");
    }
    aiInstance = new GoogleGenAI({ apiKey: apiKey || "" });
  }
  return aiInstance;
}

export interface WordInsight {
  context: string;
  mnemonic: string;
  usage: string;
  synonyms: string[];
}

export async function getWordInsight(word: string, category: string): Promise<WordInsight> {
  const ai = getAI();
  const prompt = `
    Analyze the following English word: "${word}" (Category: ${category}).
    Provide:
    1. "SSC Exam Context": Typical usage in exams.
    2. "Mnemonic": Memory trick.
    3. "Example Sentence": A clear, high-yield sentence.
    4. "Top 3 Synonyms": Comma separated.
    Return as JSON.
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

    return JSON.parse(response.text);
  } catch (error) {
    console.error("Gemini API Error:", error);
    return {
      context: "Context unavailable. Focus on core meaning.",
      mnemonic: "Repetition is key!",
      usage: `Success requires mastering words like ${word}.`,
      synonyms: ["Similar terms", "N/A"]
    };
  }
}

export async function getDailyInsight(): Promise<{ word: string, insight: string, usage: string }> {
  const ai = getAI();
  const prompt = `
    Provide a "Daily Vocabulary Pulse" for an SSC aspirant prepring for 2026.
    Pick one very important high-yield word and provide:
    1. The word itself.
    2. A "Power Insight": Why this word is crucial specifically for competitive exams.
    3. A "Modern Usage": How it might appear in a current 2026 news context.
    Return as JSON.
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
    return JSON.parse(response.text);
  } catch (error) {
    return {
      word: "Persistence",
      insight: "Consistency is more important than intensity in SSC prep.",
      usage: "The candidate's persistence in mastering idioms paid off in the final tier."
    };
  }
}
