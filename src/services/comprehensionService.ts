import { GoogleGenAI, Type } from "@google/genai";

let aiInstance: GoogleGenAI | null = null;

function getAI() {
  if (!aiInstance) {
    const apiKey = typeof process !== 'undefined' ? process.env.GEMINI_API_KEY : undefined;
    aiInstance = new GoogleGenAI({ apiKey: apiKey || "" });
  }
  return aiInstance;
}

export interface PQRSQuestion {
  id: string;
  s1?: string; // Optional fixed first sentence
  s6?: string; // Optional fixed last sentence
  sentences: string[]; // The jumbled parts (P, Q, R, S)
  correctSequence: string;
  explanation: string;
  logicalConnectors: string[];
}

export interface ClozeQuestion {
  id: string;
  passage: string;
  blanks: {
    index: number;
    options: string[];
    answer: string;
    explanation: string;
  }[];
}

export interface RCQuestion {
  id: string;
  passage: string;
  questions: {
    text: string;
    options: string[];
    answer: string;
    explanation: string;
  }[];
}

export interface DailyComprehensionData {
  date: string;
  pqrs: PQRSQuestion[];
  cloze: ClozeQuestion[]; // Changed to array
  rc: RCQuestion;
}

export async function getDailyComprehension(): Promise<DailyComprehensionData> {
  const today = new Date().toISOString().split('T')[0];
  const ai = getAI();
  
  const prompt = `
    Generate a daily English Comprehension practice set for SSC CGL Tier-II / CHSL 2026 pattern.
    
    Difficulty Level: HIGH (Vocab should be advanced, sentences complex).
    
    Required Content:
    1. 5 PQRS questions. At least 3 must be "S1 and S6" style (Fixed start and end).
    2. 2 DIFFERENT Cloze Tests with 5 blanks each (Context: Tech, Philosophy, or Economics).
    3. 1 LONG Reading Comprehension passage (450-600 words) of SSC CGL and CHSL 2026 pattern difficulty level.
       - Provide exactly 5 questions: 1 Title/Theme, 1 Inference, 1 Fact-based, 1 Vocabulary (Synonym/Antonym from context), and 1 Tone/Style question.
    
    For every question, provide a "Smart Explanation" highlighting "Grammar Rules" or "Logical Connectors".
    
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
            date: { type: Type.STRING },
            pqrs: {
              type: Type.ARRAY,
              items: {
                type: Type.OBJECT,
                properties: {
                  id: { type: Type.STRING },
                  s1: { type: Type.STRING },
                  s6: { type: Type.STRING },
                  sentences: { type: Type.ARRAY, items: { type: Type.STRING } },
                  correctSequence: { type: Type.STRING },
                  explanation: { type: Type.STRING },
                  logicalConnectors: { type: Type.ARRAY, items: { type: Type.STRING } }
                },
                required: ["id", "sentences", "correctSequence", "explanation", "logicalConnectors"]
              }
            },
            cloze: {
              type: Type.ARRAY, // Changed to array
              items: {
                type: Type.OBJECT,
                properties: {
                  id: { type: Type.STRING },
                  passage: { type: Type.STRING },
                  blanks: {
                    type: Type.ARRAY,
                    items: {
                      type: Type.OBJECT,
                      properties: {
                        index: { type: Type.NUMBER },
                        options: { type: Type.ARRAY, items: { type: Type.STRING } },
                        answer: { type: Type.STRING },
                        explanation: { type: Type.STRING }
                      },
                      required: ["index", "options", "answer", "explanation"]
                    }
                  }
                },
                required: ["id", "passage", "blanks"]
              }
            },
            rc: {
              type: Type.OBJECT,
              properties: {
                id: { type: Type.STRING },
                passage: { type: Type.STRING },
                questions: {
                  type: Type.ARRAY,
                  items: {
                    type: Type.OBJECT,
                    properties: {
                      text: { type: Type.STRING },
                      options: { type: Type.ARRAY, items: { type: Type.STRING } },
                      answer: { type: Type.STRING },
                      explanation: { type: Type.STRING }
                    },
                    required: ["text", "options", "answer", "explanation"]
                  }
                }
              },
              required: ["id", "passage", "questions"]
            }
          },
          required: ["date", "pqrs", "cloze", "rc"]
        }
      }
    });

    return JSON.parse(response.text || "{}");
  } catch (error) {
    console.error("Failed to fetch daily comprehension:", error);
    return getFallbackData(today);
  }
}

function getFallbackData(date: string): DailyComprehensionData {
  return {
    date,
    pqrs: [
      {
        id: "f1",
        s1: "S1: The rise of automation has sparked intensive debate regarding the future of manual labor.",
        s6: "S6: Ultimately, a nuanced approach is required to navigate this transition effectively.",
        sentences: [
          "P: Proponents argue that AI can increase efficiency and democratize access to high-end services.",
          "Q: Critics, however, warn of potential algorithmic discrimination and job displacement.",
          "R: This tension lies at the very heart of the modern technological paradigm.",
          "S: Furthermore, the lack of transparency in many AI systems complicates public trust."
        ],
        correctSequence: "PQSR",
        explanation: "S1 sets the theme. P provides the first argument. Q provides a contrast with 'however'. S adds a related point with 'Furthermore'. R summarizes the conflict before S6.",
        logicalConnectors: ["However", "Furthermore"]
      }
    ],
    cloze: [
      {
        id: "c1",
        passage: "The structural (1) of post-colonial infrastructure often manifests as a simultaneity of obsolescence and futurity. Flyovers calcified mid-renovation do not signify simple (2) but rather a disjunctive temporality. Here, functionality is (3) to the point. What matters is the (4) illusion of development. This spatial discourse (5) the state's promises into sedimented form.",
        blanks: [
          { index: 1, options: ["tenacity", "attrition", "malaise", "fixation"], answer: "attrition", explanation: "Context of decay and wearing down." },
          { index: 2, options: ["repair", "malfunction", "success", "design"], answer: "malfunction", explanation: "Negative state of infrastructure." },
          { index: 3, options: ["beside", "central", "critical", "pivotal"], answer: "beside", explanation: "Idiom 'beside the point' meaning irrelevant." },
          { index: 4, options: ["vivid", "performative", "actual", "meager"], answer: "performative", explanation: "Acting out a role rather than being real." },
          { index: 5, options: ["materializes", "dissolves", "ignores", "reifies"], answer: "materializes", explanation: "Turning abstract promises into physical form." }
        ]
      },
      {
        id: "c2",
        passage: "Cybersecurity (1) is no longer a luxury but a fundamental necessity. In an era where data is the new currency, malicious (2) often target vulnerable points in a network. A robust (3) architecture involves multi-layered encryption. Organizations must (4) their employees to recognize phishing attempts. Ultimately, (5) is a collective responsibility.",
        blanks: [
          { index: 1, options: ["optional", "redundancy", "hygiene", "excess"], answer: "hygiene", explanation: "Common term for security practices." },
          { index: 2, options: ["entities", "actors", "users", "groups"], answer: "actors", explanation: "Common term in cybersecurity (threat actors)." },
          { index: 3, options: ["static", "defense", "loose", "minor"], answer: "defense", explanation: "Context of security layers." },
          { index: 4, options: ["ignore", "fire", "educate", "reward"], answer: "educate", explanation: "Critical for phishing prevention." },
          { index: 5, options: ["profit", "speed", "resilience", "growth"], answer: "resilience", explanation: "Overall goal of security." }
        ]
      }
    ],
    rc: {
      id: "r1",
      passage: "In the contemporary corporate milieu, the ascendancy of women occupying the echelons of Chief Executive Officer (CEO) and Chief Technology Officer (CTO) roles has increasingly disrupted the traditionally male-dominated paradigm. Despite systemic impediments such as gender bias and glass ceilings, a growing cohort of women leaders is redefining organizational dynamics through innovative leadership and technological acumen. The emergence of women as leaders heralds a nuanced reconfiguration of power structures. Their leadership is often characterized by a collaborative approach, emotional intelligence, and resilience, which synergize effectively with technological innovation and business strategy. However, these trailblazing women frequently confront challenges including unequal access to mentorship, networking opportunities, and unconscious biases embedded within corporate cultures. The persistence of such barriers underscores the necessity for institutional reforms aimed at amplifying female representation in executive roles. Moreover, the integration of technology with visionary leadership by women CTOs has catalyzed transformative advancements across sectors, underscoring the symbiotic relationship between gender diversity and technological progress. This paradigm not only enhances organizational performance but also fosters sustainable development goals aligned with equity. In essence, the emergence of women as CEOs and CTOs heralds a future where gender parity and innovation coalesce to propel economic and social progress.",
      questions: [
        { text: "What is the primary focus of the passage?", options: ["The history of corporate boardrooms", "The role of women in redefining corporate dynamics", "The failure of male leaders in the tech industry", "The mechanics of technological innovation"], answer: "The role of women in redefining corporate dynamics", explanation: "The text centers on how women leaders are changing power structures and organizational dynamics." },
        { text: "What does 'nuanced reconfiguration' imply in context?", options: ["A sudden and violent shift", "A subtle and complex restructuring", "A return to traditional values", "A complete stop to progress"], answer: "A subtle and complex restructuring", explanation: "It refers to refined, layered changes in power structures." },
        { text: "Which challenge is NOT explicitly mentioned for women leaders?", options: ["Unequal access to mentorship", "Unconscious biases", "Lack of technical education", "Glass ceilings"], answer: "Lack of technical education", explanation: "The passage notes their 'technological acumen' rather than a lack of it." },
        { text: "What is implied as a result of the integration of technology and female leadership?", options: ["It leads to increased conflict", "It hinders economic progress", "It catalyzes transformative advancements", "It creates more administrative burden"], answer: "It catalyzes transformative advancements", explanation: "The passage states this combination has catalyzed advancements across sectors." }
      ]
    }
  };
}
