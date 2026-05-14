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
}

export async function getWordInsight(word: string, category: string): Promise<WordInsight> {
  const ai = getAI();
  const prompt = `
    Analyze the following English word: "${word}" (Category: ${category}).
    Provide two things:
    1. A brief "SSC Exam Context": How this word has been used in previous SSC (CHSL/CGL) exams or its typical usage patterns in such exams for 2026 preparation.
    2. A clever Mnemonic: A memory trick to remember its meaning.
    Return the response in valid JSON.
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
          },
          required: ["context", "mnemonic"],
        },
      },
    });

    return JSON.parse(response.text);
  } catch (error) {
    console.error("Gemini API Error:", error);
    return {
      context: "Context unavailable at the moment. Focus on the core meaning for now.",
      mnemonic: "Memory is a muscle. Repeat the word 5 times to lock it in!",
    };
  }
}
