import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import confetti from 'canvas-confetti';
import { Trophy, Menu, X, Info, Sun, Moon, Download, Upload, ChevronLeft, ChevronRight, BookOpen, Zap, Star, LayoutGrid, Award, BrainCircuit, Sparkles, MessageSquareQuote, CheckCircle2, RotateCw } from 'lucide-react';
import { getWordInsight, WordInsight } from './services/geminiService';

interface VocabItem {
  id: string;
  w: string;
  a: string;
  h: string;
}

interface VocabData {
  ow: VocabItem[];
  sy: VocabItem[];
  id: VocabItem[];
  pv: VocabItem[];
}

type AppMode = 'quiz' | 'learn';
type Category = 'ow' | 'sy' | 'id' | 'pv';

type ThemeKey = 'deepsea' | 'evergreen' | 'parchment' | 'nordic';

type AccentKey = 'blue' | 'cyan' | 'emerald' | 'rose' | 'amber' | 'violet';

const ACCENTS: Record<AccentKey, { text: string; bg: string; border: string; shadow: string; dot: string }> = {
  blue: { text: 'text-blue-500', bg: 'bg-blue-600', border: 'border-blue-500', shadow: 'shadow-blue-500/20', dot: 'bg-blue-500' },
  cyan: { text: 'text-cyan-400', bg: 'bg-cyan-500', border: 'border-cyan-400', shadow: 'shadow-cyan-500/20', dot: 'bg-cyan-400' },
  emerald: { text: 'text-emerald-500', bg: 'bg-emerald-600', border: 'border-emerald-500', shadow: 'shadow-emerald-500/20', dot: 'bg-emerald-500' },
  rose: { text: 'text-rose-500', bg: 'bg-rose-600', border: 'border-rose-500', shadow: 'shadow-rose-500/20', dot: 'bg-rose-500' },
  amber: { text: 'text-amber-500', bg: 'bg-amber-600', border: 'border-amber-500', shadow: 'shadow-amber-500/20', dot: 'bg-amber-500' },
  violet: { text: 'text-violet-500', bg: 'bg-violet-600', border: 'border-violet-500', shadow: 'shadow-violet-500/20', dot: 'bg-violet-500' },
};

interface ThemeConfig {
  bg: string;
  card: string;
  border: string;
  text: string;
  secondary: string;
  nav: string;
  optionBase: string;
}

const THEMES: Record<ThemeKey, ThemeConfig> = {
  deepsea: {
    bg: 'bg-[#0d1117]', 
    card: 'bg-[#161b22]', 
    border: 'border-[#30363d]',
    text: 'text-[#c9d1d9]', 
    secondary: 'bg-[#21262d]',
    nav: 'bg-[#0d1117]/95',
    optionBase: 'bg-[#161b22] border-[#30363d]'
  },
  evergreen: {
    bg: 'bg-[#0b120f]', 
    card: 'bg-[#141f1a]',
    border: 'border-[#23352e]',
    text: 'text-[#d1d9d1]',
    secondary: 'bg-[#1a2b24]',
    nav: 'bg-[#0b120f]/95',
    optionBase: 'bg-[#141f1a] border-[#23352e]'
  },
  parchment: {
    bg: 'bg-[#f4ead5]',
    card: 'bg-[#faf3e0]',
    border: 'border-[#e0d6c0]',
    text: 'text-[#4a3728]',
    secondary: 'bg-[#e9dec6]',
    nav: 'bg-[#f4ead5]/90',
    optionBase: 'bg-[#fffdfa] border-[#dcd2bb]'
  },
  nordic: {
    bg: 'bg-[#f8fafc]',
    card: 'bg-white',
    border: 'border-slate-200',
    text: 'text-slate-800',
    secondary: 'bg-slate-100',
    nav: 'bg-white/90',
    optionBase: 'bg-white border-slate-300'
  }
};

interface QuizCardProps {
  item: VocabItem;
  idx: number;
  cat: Category;
  database: VocabData;
  answered: Record<number, { selected: string; correct: boolean }>;
  showHint: Record<number, boolean>;
  onAnswer: (qIdx: number, item: VocabItem, choice: string) => void;
  onToggleHint: (idx: number) => void;
  theme: ThemeConfig;
  themeKey: ThemeKey;
  accent: { text: string; bg: string; border: string; shadow: string };
  globalSerial: number;
}

interface Achievement {
  id: string;
  title: string;
  desc: string;
  icon: React.ReactNode;
  unlocked: boolean;
}

const QuizCard: React.FC<QuizCardProps> = ({ item, idx, cat: parentCat, database, answered, showHint, onAnswer, onToggleHint, theme, themeKey, accent, globalSerial }) => {
  const [insight, setInsight] = useState<WordInsight | null>(null);
  const [loadingAI, setLoadingAI] = useState(false);
  const options = useMemo(() => {
    // Detect actual category from ID prefix
    let actualCat: Category = parentCat;
    if (item.id.startsWith('ow')) actualCat = 'ow';
    else if (item.id.startsWith('sy')) actualCat = 'sy';
    else if (item.id.startsWith('id')) actualCat = 'id';
    else if (item.id.startsWith('pv')) actualCat = 'pv';

    const allPotential = (database[actualCat] || []).filter(x => x.id !== item.id);
    
    // Smart similarity scoring
    const scored = allPotential.map(candidate => {
      let score = 0;
      const cWord = candidate.a.toLowerCase().trim();
      const iWord = item.a.toLowerCase().trim();
      
      // 1. Prefix match (Very strong similarity)
      if (cWord.slice(0, 4) === iWord.slice(0, 4)) score += 120;
      else if (cWord.slice(0, 3) === iWord.slice(0, 3)) score += 80;
      else if (cWord.slice(0, 2) === iWord.slice(0, 2)) score += 40;
      else if (cWord[0] === iWord[0]) score += 15;

      // 2. Length parity
      const lenDiff = Math.abs(cWord.length - iWord.length);
      if (lenDiff === 0) score += 20;
      else if (lenDiff <= 2) score += 10;

      // 3. Suffix match (Common for phrasal verbs or similar word endings)
      if (cWord.slice(-3) === iWord.slice(-3)) score += 35;
      else if (cWord.slice(-2) === iWord.slice(-2)) score += 15;

      // 4. Character overlap
      const cChars = new Set(cWord);
      const iChars = new Set(iWord);
      const intersection = [...cChars].filter(x => iChars.has(x));
      score += (intersection.length / Math.max(cChars.size, iChars.size)) * 50;

      // Add a bit of randomness to the score to keep it fresh
      score += Math.random() * 10;

      return { word: candidate.a, score };
    });

    const distractors = scored
      .sort((a, b) => b.score - a.score)
      .slice(0, 15) // Pick from top 15 most similar
      .sort(() => 0.5 - Math.random()) // Shuffle those
      .slice(0, 3)
      .map(x => x.word);

    // Filter duplicates to avoid React key errors if same word exists with different meanings
    return Array.from(new Set([item.a, ...distractors])).sort(() => 0.5 - Math.random());
  }, [item.id, parentCat, database]);

  const state = answered[idx];

  return (
    <motion.div 
      initial={{ y: 20, opacity: 0 }}
      whileInView={{ y: 0, opacity: 1 }}
      viewport={{ once: true, margin: "-50px" }}
      id={`card-${idx}`} 
      className={`border rounded-[2.5rem] p-6 relative shadow-xl transition-all duration-500 overflow-hidden ${theme.card} ${theme.border} group`}
    >
      <div 
        className={`absolute top-0 left-0 w-1 h-full opacity-30 ${accent.bg}`}
      />
      
      <div 
        className={`absolute top-5 right-6 cursor-pointer hover:opacity-100 transition-all p-2 rounded-full hover:bg-black/10 ${accent.text} opacity-60`} 
        onClick={() => onToggleHint(idx)}
      >
        <Info size={18} />
      </div>

      <div className="flex items-center gap-2 mb-2">
        <span className="text-[10px] opacity-40 font-black uppercase tracking-widest italic flex items-center gap-1">
          <BookOpen size={10} /> #{globalSerial}
        </span>
      </div>

      <h3 className={`text-2xl font-black mt-2 mb-10 text-center leading-tight font-serif ${accent.text} tracking-tight`}>
        {item.w}
      </h3>

      <div className="grid grid-cols-1 gap-3">
        {options.map((opt, oIdx) => {
          const isSelected = state?.selected === opt;
          const isCorrect = item.a === opt;
          
          let colorClass = `${theme.optionBase} ${theme.text} hover:scale-[1.02] hover:shadow-lg active:scale-[0.98]`;

          if (state) {
            if (isCorrect) colorClass = 'bg-emerald-500/20 border-emerald-500 text-emerald-500 font-black shadow-lg shadow-emerald-500/10 ring-1 ring-emerald-500';
            else if (isSelected) colorClass = 'bg-rose-500/20 border-rose-500 text-rose-500 font-black shadow-lg shadow-rose-500/10 ring-1 ring-rose-500';
            else colorClass = 'opacity-30 grayscale scale-[0.98] pointer-events-none';
          }

          return (
            <button 
              key={opt}
              disabled={!!state}
              onClick={() => onAnswer(idx, item, opt)}
              className={`w-full text-left py-5 px-6 rounded-2xl text-sm font-bold border transition-all duration-300 relative group/btn ${colorClass} flex items-center justify-between`}
            >
              <span className="flex-1">{opt}</span>
              {!state && (
                <span className="text-[9px] opacity-0 group-hover/btn:opacity-40 transition-opacity font-black uppercase tracking-tighter">
                  Choice {oIdx + 1}
                </span>
              )}
              {state && isCorrect && <Star size={16} fill="currentColor" />}
              {state && isSelected && !isCorrect && <X size={16} />}
            </button>
          );
        })}
      </div>

      <AnimatePresence>
        {state && (
          <motion.div 
            initial={{ y: 10, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            className="mt-6 flex flex-col gap-3"
          >
            <div className="flex gap-2">
              <button 
                onClick={async () => {
                  if (insight) { setInsight(null); return; }
                  setLoadingAI(true);
                  const res = await getWordInsight(item.a, parentCat);
                  setInsight(res);
                  setLoadingAI(false);
                }}
                disabled={loadingAI}
                className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-2xl text-[10px] font-black uppercase tracking-widest border border-white/10 ${theme.secondary} hover:bg-white/5 transition-colors relative overflow-hidden`}
              >
                {loadingAI && (
                  <motion.div 
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="absolute inset-0 bg-indigo-600/30 flex items-center justify-center backdrop-blur-md z-10"
                  >
                    <BrainCircuit size={16} className="animate-pulse text-white mr-2" />
                    <span className="text-[8px] text-white">AI THINKING...</span>
                  </motion.div>
                )}
                <Sparkles size={14} className={insight ? 'text-amber-400' : ''} />
                {insight ? 'Hide AI Insight' : 'AI powered Context'}
              </button>
            </div>

            {insight && (
              <motion.div 
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                className={`p-5 rounded-3xl ${theme.secondary} border border-white/5 space-y-4 relative`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className={`text-[9px] font-black uppercase tracking-[0.2em] flex items-center gap-2 ${accent.text}`}>
                    <LayoutGrid size={10} /> SSC Exam Context 2026
                  </span>
                  <button 
                    onClick={async (e) => {
                      e.stopPropagation();
                      setLoadingAI(true);
                      const res = await getWordInsight(item.a, parentCat, true);
                      setInsight(res);
                      setLoadingAI(false);
                    }}
                    disabled={loadingAI}
                    className="p-1 rounded-md hover:bg-white/10 transition-colors opacity-40 hover:opacity-100"
                  >
                    <RotateCw size={10} className={loadingAI ? 'animate-spin' : ''} />
                  </button>
                </div>
                <p className="text-xs leading-relaxed opacity-70 italic">"{insight.context}"</p>
                <div className="h-px bg-white/5" />
                <div className="space-y-1">
                   <span className={`text-[9px] font-black uppercase tracking-[0.2em] flex items-center gap-2 text-indigo-400`}>
                    <MessageSquareQuote size={10} /> Practical Usage
                  </span>
                  <p className="text-xs opacity-80 font-medium">"{insight.usage}"</p>
                </div>
                <div className="h-px bg-white/5" />
                <div className="space-y-1">
                  <span className={`text-[9px] font-black uppercase tracking-[0.2em] flex items-center gap-2 text-amber-500`}>
                    <BrainCircuit size={10} /> Mnemonic Device
                  </span>
                  <p className="text-sm font-bold tracking-tight">{insight.mnemonic}</p>
                </div>
                {insight.synonyms && insight.synonyms.length > 0 && (
                  <>
                    <div className="h-px bg-white/5" />
                    <div className="flex flex-wrap gap-1">
                      {insight.synonyms.map((s, si) => (
                        <span key={si} className="text-[8px] bg-white/5 px-2 py-0.5 rounded-full font-bold uppercase tracking-widest">{s}</span>
                      ))}
                    </div>
                  </>
                )}
              </motion.div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showHint[idx] && (
          <motion.div 
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className={`mt-4 p-4 border-l-4 ${accent.border} rounded-lg text-sm transition-colors ${themeKey === 'parchment' || themeKey === 'nordic' ? 'bg-slate-100 text-slate-600' : 'bg-black/20 text-slate-300'}`}
          >
            <span className={`${accent.text} font-bold text-[10px] uppercase block mb-1`}>Hindi Meaning</span>
            {item.h}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}

interface LearnCardProps {
  item: VocabItem;
  cat: Category;
  globalSerial: number;
  theme: ThemeConfig;
  accent: { text: string; bg: string; border: string; shadow: string };
}

const LearnCard: React.FC<LearnCardProps> = ({ item, cat, globalSerial, theme, accent }) => {
  const [insight, setInsight] = useState<WordInsight | null>(null);
  const [loadingAI, setLoadingAI] = useState(false);

  return (
    <div key={item.id} className={`p-4 rounded-[1.5rem] border transition-all shadow-sm ${theme.card} ${theme.border}`}>
      <div className="flex justify-between items-start mb-1">
        <h4 className={`text-lg font-bold font-serif ${accent.text}`}>{item.w}</h4>
        <span className="text-[10px] font-black opacity-30 italic">#{globalSerial}</span>
      </div>
      <p className={`text-sm font-medium leading-relaxed opacity-80 mb-4`}>{item.a}</p>
      
      <div className="flex flex-col gap-3">
        <button 
          onClick={async () => {
            if (insight) { setInsight(null); return; }
            setLoadingAI(true);
            const res = await getWordInsight(item.a, cat);
            setInsight(res);
            setLoadingAI(false);
          }}
          disabled={loadingAI}
          className={`w-full flex items-center justify-center gap-2 py-2 rounded-xl text-[9px] font-black uppercase tracking-widest border border-white/5 ${theme.secondary} hover:bg-white/5 transition-colors relative overflow-hidden`}
        >
          {loadingAI && (
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="absolute inset-0 bg-indigo-600/30 flex items-center justify-center backdrop-blur-md z-10"
            >
              <BrainCircuit size={16} className="animate-pulse text-white mr-2" />
              <span className="text-[8px] text-white">GENERATING AI INSIGHT...</span>
            </motion.div>
          )}
          <Sparkles size={14} className={insight ? 'text-amber-400' : ''} />
          {insight ? 'Hide AI Context' : 'AI Context & Mnemonic'}
        </button>

        {insight && (
          <motion.div 
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`p-4 rounded-2xl ${theme.secondary} border border-white/5 space-y-3 relative`}
          >
            <div className="flex items-center justify-between gap-2">
              <span className={`text-[8px] font-black uppercase tracking-[0.2em] flex items-center gap-2 ${accent.text}`}>
                <LayoutGrid size={10} /> Exam Context 2026
              </span>
              <button 
                onClick={async (e) => {
                  e.stopPropagation();
                  setLoadingAI(true);
                  const res = await getWordInsight(item.a, cat, true);
                  setInsight(res);
                  setLoadingAI(false);
                }}
                disabled={loadingAI}
                className="p-1 rounded-md hover:bg-white/10 transition-colors opacity-40 hover:opacity-100"
              >
                <RotateCw size={10} className={loadingAI ? 'animate-spin' : ''} />
              </button>
            </div>
            <p className="text-[10px] leading-relaxed opacity-70 italic">{insight.context}</p>
            <div className="h-px bg-white/5" />
            <div className="space-y-1">
                <span className={`text-[8px] font-black uppercase tracking-[0.2em] flex items-center gap-2 text-indigo-400`}>
                <MessageSquareQuote size={10} /> Usage example
              </span>
              <p className="text-[9px] opacity-80 font-medium italic">"{insight.usage}"</p>
            </div>
            <div className="h-px bg-white/5" />
            <div className="space-y-1">
              <span className={`text-[8px] font-black uppercase tracking-[0.2em] flex items-center gap-2 text-amber-500`}>
                <BrainCircuit size={10} /> Mnemonic Device
              </span>
              <p className="text-[10px] font-bold tracking-tight">{insight.mnemonic}</p>
            </div>
          </motion.div>
        )}
      </div>

      <div className={`mt-3 pt-3 border-t ${theme.border} opacity-40 flex justify-between items-center text-xs`}>
        <p className={`font-bold uppercase tracking-widest flex items-center gap-2 ${accent.text}`}>
          <Star size={12} /> {item.h}
        </p>
      </div>
    </div>
  );
};

export default function App() {
  const SET_SIZE = 50;
  const TOTAL_PLAN_DAYS = 75;

  const [mode, setMode] = useState<AppMode>('quiz');
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [cat, setCat] = useState<Category>('ow');
  const [categoryLastSets, setCategoryLastSets] = useState<Record<Category, number>>({ ow: 0, sy: 0, id: 0, pv: 0 });
  const curSet = categoryLastSets[cat] || 0;
  const setCurSet = (val: number, targetCat?: Category) => setCategoryLastSets(prev => ({ ...prev, [targetCat || cat]: val }));
  const [streak, setStreak] = useState(3);
  const [themeKey, setThemeKey] = useState<ThemeKey>('deepsea');
  const [accentKey, setAccentKey] = useState<AccentKey>('blue');
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isWeakRevision, setIsWeakRevision] = useState(false);
  const [isChallengeMode, setIsChallengeMode] = useState(false);
  const [challengeDay, setChallengeDay] = useState(1);
  
  const [answered, setAnswered] = useState<Record<number, { selected: string; correct: boolean }>>({});
  const [completedSets, setCompletedSets] = useState<Record<Category, number[]>>({ ow: [], sy: [], id: [], pv: [] });
  const [completedChallengeDays, setCompletedChallengeDays] = useState<number[]>([]);
  const [activityHistory, setActivityHistory] = useState<Record<string, number>>({});
  const [achievements, setAchievements] = useState<string[]>([]);
  const [weakList, setWeakList] = useState<Record<Category, VocabItem[]>>({ ow: [], sy: [], id: [], pv: [] });
  const [showResults, setShowResults] = useState(false);
  const [showHint, setShowHint] = useState<Record<number, boolean>>({});
  const [database, setDatabase] = useState<VocabData | null>(null);

  const activeBatch = useMemo(() => {
    if (!database) return [];
    if (isWeakRevision) {
       return weakList[cat];
    }
    if (isChallengeMode) {
      const quotas: Record<Category, number> = { ow: 27, sy: 24, id: 24, pv: 7 };
      const list = database[cat] || [];
      const perDay = quotas[cat];
      const start = (challengeDay - 1) * perDay;
      const end = challengeDay * perDay;
      return list.slice(start, end);
    }
    let list = database[cat];
    const curSet = categoryLastSets[cat] || 0;
    return list.slice(curSet * SET_SIZE, (curSet + 1) * SET_SIZE);
  }, [cat, categoryLastSets, database, isWeakRevision, weakList, isChallengeMode, challengeDay]);

  const totalSets = useMemo(() => {
    if (!database) return 0;
    return Math.ceil(database[cat].length / SET_SIZE);
  }, [cat, database]);

  const theme = THEMES[themeKey] || THEMES.deepsea;
  const accent = ACCENTS[accentKey] || ACCENTS.blue;

  const stats = useMemo(() => {
    if (!database) return { total: 0, mastered: 0 };
    const total = database.ow.length + database.sy.length + database.id.length + database.pv.length;
    const mastered = completedSets.ow.length * SET_SIZE + completedSets.sy.length * SET_SIZE + completedSets.id.length * SET_SIZE + completedSets.pv.length * SET_SIZE;
    return { total, mastered };
  }, [database, completedSets]);

  const masteredCount = useMemo(() => {
    return Object.values(completedSets).flat().length;
  }, [completedSets]);

  const currentScore = useMemo(() => {
    return (Object.values(answered) as { correct: boolean }[]).filter(a => a.correct).length;
  }, [answered]);

  const allAnswered = useMemo(() => {
    return activeBatch.length > 0 && Object.keys(answered).length === activeBatch.length;
  }, [activeBatch, answered]);

  const currentQuestionIdx = useMemo(() => {
    return activeBatch.findIndex((_, i) => !answered[i]);
  }, [activeBatch, answered]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showResults || !activeBatch.length || isSidebarOpen) return;
      if (['1', '2', '3', '4'].includes(e.key)) {
        const choiceIdx = parseInt(e.key) - 1;
        if (currentQuestionIdx !== -1) {
          // We don't have direct access to 'options' here easily because they are shuffled in QuizCard
        }
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeBatch, answered, showResults, currentQuestionIdx, isSidebarOpen]);

  useEffect(() => {
    fetch('/assets/data.json')
      .then(res => res.json())
      .then(data => setDatabase(data))
      .catch(err => console.error('Failed to load vocab data:', err));

    const saved = localStorage.getItem('jt_vocab_v2');
    if (saved) {
      const parsed = JSON.parse(saved);
      setStreak(parsed.streak ?? 3);
      setCompletedSets(parsed.completedSets || { ow: [], sy: [], id: [], pv: [] });
      setWeakList(parsed.weakList || { ow: [], sy: [], id: [], pv: [] });
      setCompletedChallengeDays(parsed.completedChallengeDays || []);
      setActivityHistory(parsed.activityHistory || {});
      setAchievements(parsed.achievements || []);
      setChallengeDay(parsed.challengeDay || 1);
      if (parsed.themeKey && THEMES[parsed.themeKey as ThemeKey]) {
        setThemeKey(parsed.themeKey as ThemeKey);
      }
      if (parsed.accentKey && ACCENTS[parsed.accentKey as AccentKey]) {
        setAccentKey(parsed.accentKey as AccentKey);
      }
      if (parsed.cat) {
        setCat(parsed.cat as Category);
      }
      if (parsed.categoryLastSets) {
        setCategoryLastSets(parsed.categoryLastSets);
      } else if (typeof parsed.curSet === 'number') {
        // Migration for old structure
        setCategoryLastSets(prev => ({ ...prev, [parsed.cat || 'ow']: parsed.curSet }));
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('jt_vocab_v2', JSON.stringify({ 
      streak, 
      completedSets, 
      weakList, 
      themeKey,
      accentKey,
      completedChallengeDays,
      activityHistory,
      achievements,
      challengeDay,
      cat,
      categoryLastSets
    }));
  }, [streak, completedSets, weakList, themeKey, accentKey, completedChallengeDays, activityHistory, achievements, challengeDay, cat, categoryLastSets]);

  const handleAnswer = (qIdx: number, item: VocabItem, choice: string) => {
    if (answered[qIdx]) return;
    const isCorrect = choice === item.a;

    // Detect actual category from ID
    let itemCat: Category = 'ow';
    if (item.id.startsWith('ow')) itemCat = 'ow';
    if (item.id.startsWith('sy')) itemCat = 'sy';
    if (item.id.startsWith('id')) itemCat = 'id';
    if (item.id.startsWith('pv')) itemCat = 'pv';

    setAnswered(prev => ({ ...prev, [qIdx]: { selected: choice, correct: isCorrect } }));
    
    if (!isCorrect) {
      setWeakList(prev => {
        if (prev[itemCat].find(x => x.id === item.id)) return prev;
        return { ...prev, [itemCat]: [...prev[itemCat], item] };
      });
    } else if (isWeakRevision && isCorrect) {
      setWeakList(prev => ({
        ...prev,
        [itemCat]: prev[itemCat].filter(x => x.id !== item.id)
      }));
    }
  };

  const finishSession = () => {
    const correctCount = (Object.values(answered) as { correct: boolean }[]).filter(a => a.correct).length;
    
    // For learn mode, we assume 100% progress if they finish
    const isLearnFinished = mode === 'learn' && !isWeakRevision && !isChallengeMode;
    const threshold = activeBatch.length * 0.8;

    if (isChallengeMode) {
      if (correctCount >= activeBatch.length * 0.9) {
        setCompletedChallengeDays(prev => {
          if (prev.includes(challengeDay)) return prev;
          return [...prev, challengeDay].sort((a, b) => a - b);
        });
      }
    } else if (!isWeakRevision && (correctCount >= threshold || isLearnFinished)) {
       setCompletedSets(prev => {
         const curSet = categoryLastSets[cat] || 0;
         const currentCompleted = prev[cat] || [];
         if (currentCompleted.includes(curSet)) return prev;
         return { ...prev, [cat]: [...currentCompleted, curSet] };
       });
    }
    setShowResults(true);
    
    if ((correctCount === activeBatch.length || isLearnFinished) && activeBatch.length > 0) {
      confetti({ particleCount: 150, spread: 70, origin: { y: 0.6 } });
    }

    // Update Achievement & History
    const today = new Date().toISOString().split('T')[0];
    setActivityHistory(prev => ({
      ...prev,
      [today]: (prev[today] || 0) + activeBatch.length
    }));

    // Achievement Checks
    const newAchievements: string[] = [];
    if ((correctCount === activeBatch.length || isLearnFinished) && activeBatch.length >= 20) newAchievements.push('perfectionist');
    if (stats.mastered + (isLearnFinished ? activeBatch.length : correctCount) >= 100 && !achievements.includes('centurion')) newAchievements.push('centurion');
    if (completedChallengeDays.length >= 7 && !achievements.includes('consistent')) newAchievements.push('consistent');
    if ((isLearnFinished || correctCount / activeBatch.length >= 0.9) && !achievements.includes('scholar')) newAchievements.push('scholar');
    
    if (newAchievements.length > 0) {
      setAchievements(prev => [...new Set([...prev, ...newAchievements])]);
    }
  };

  if (!database) {
    return (
      <div className={`min-h-screen flex flex-col items-center justify-center ${theme.bg}`}>
        <motion.div 
          animate={{ 
            scale: [1, 1.1, 1],
            opacity: [0.3, 1, 0.3]
          }}
          transition={{ repeat: Infinity, duration: 2 }}
          className={`text-4xl font-black italic tracking-tighter mb-8 ${accent.text}`}
        >
          JT VOCAB
        </motion.div>
        <div className="w-48 h-1 bg-black/20 rounded-full overflow-hidden">
          <motion.div 
            initial={{ x: '-100%' }}
            animate={{ x: '100%' }}
            transition={{ repeat: Infinity, duration: 1.5, ease: 'easeInOut' }}
            className={`w-full h-full ${accent.bg}`}
          />
        </div>
      </div>
    );
  }

  const getRank = (score: number, total: number) => {
    const p = total > 0 ? (score / total) * 100 : 0;
    if (p === 100) return { label: 'LEGENDARY', color: 'text-amber-500' };
    if (p >= 90) return { label: 'GRANDMASTER', color: 'text-violet-500' };
    if (p >= 75) return { label: 'SCHOLAR', color: 'text-blue-500' };
    if (p >= 50) return { label: 'PRACTITIONER', color: 'text-emerald-500' };
    return { label: 'NOVICE', color: 'text-slate-500' };
  };

  const ACHIEVEMENTS: Achievement[] = [
    { id: 'perfectionist', title: 'Perfectionist', desc: '100% score in a large set', icon: <Star className="text-amber-400" /> , unlocked: achievements.includes('perfectionist') },
    { id: 'centurion', title: 'Centurion', desc: 'Mastered 100+ words', icon: <Trophy className="text-violet-400" />, unlocked: achievements.includes('centurion') },
    { id: 'consistent', title: 'Survivor', desc: 'Completed 7 days of 75 Plan', icon: <Zap className="text-blue-400" />, unlocked: achievements.includes('consistent') },
    { id: 'scholar', title: 'Grand Scholar', desc: 'Score GRANDMASTER rank', icon: <BookOpen className="text-emerald-400" />, unlocked: achievements.includes('scholar') },
  ];

  return (
    <div className={`min-h-screen font-sans transition-all duration-500 overflow-x-hidden ${theme.bg} ${theme.text} relative`}>
      {/* Background Dynamic Effects */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden z-0">
        <motion.div 
          animate={{ 
            opacity: [0.03, 0.05, 0.03],
          }}
          transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
          className={`absolute -top-1/4 -left-1/4 w-full h-full rounded-full blur-[100px] ${accent.bg} opacity-10`}
        />
        <motion.div 
          animate={{ 
            opacity: [0.02, 0.04, 0.02],
          }}
          transition={{ duration: 15, repeat: Infinity, ease: "easeInOut" }}
          className={`absolute -bottom-1/4 -right-1/4 w-full h-full rounded-full blur-[100px] ${accent.bg} opacity-5`}
        />
      </div>
      
      {/* Sidebar Drawer */}
      <AnimatePresence>
        {isSidebarOpen && (
          <div className="fixed inset-0 z-50">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsSidebarOpen(false)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className={`absolute top-0 left-0 bottom-0 w-[75%] max-w-xs p-6 flex flex-col shadow-2xl ${theme.card} ${theme.text}`}
            >
              <div className="flex justify-between items-center mb-10">
                <h2 className={`text-lg font-black ${accent.text} tracking-tight italic`}>JT DASHBOARD</h2>
                <button onClick={() => setIsSidebarOpen(false)} className="opacity-50 hover:opacity-100 p-2"><X size={20} /></button>
              </div>

              <div className="flex flex-col gap-2">
                <button 
                  onClick={() => {
                    setMode('quiz');
                    setIsChallengeMode(false);
                    setIsWeakRevision(false);
                    setIsSidebarOpen(false);
                    setShowResults(false);
                  }}
                  className={`flex items-center gap-3 p-4 rounded-3xl transition-all ${mode === 'quiz' && !isChallengeMode && !isWeakRevision ? `${accent.bg} text-white shadow-lg` : 'hover:bg-white/5 opacity-60 hover:opacity-100'}`}
                >
                  <LayoutGrid size={20} />
                  <div className="text-left">
                    <p className="text-xs font-black uppercase tracking-widest">Vocab Practice</p>
                    <p className="text-[9px] opacity-70">{cat.toUpperCase()} / Set {curSet + 1}</p>
                  </div>
                </button>

                <div className="flex flex-col gap-2 p-2 bg-black/5 rounded-3xl">
                  <p className="text-[9px] font-black uppercase tracking-widest opacity-40 px-2 mt-1">Vocab Categories</p>
                  <div className="grid grid-cols-2 gap-2">
                    {[
                      { id: 'ow', label: 'OWS' },
                      { id: 'sy', label: 'Synonyms' },
                      { id: 'id', label: 'Idioms' },
                      { id: 'pv', label: 'Phrasal' }
                    ].map(item => (
                      <button 
                        key={item.id}
                        onClick={() => { 
                          setCat(item.id as Category); 
                          setAnswered({}); 
                          setShowResults(false);
                          setMode('quiz');
                          setIsWeakRevision(false);
                          setIsSidebarOpen(false);
                        }}
                        className={`px-3 py-2 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all border ${cat === item.id && mode === 'quiz' && !isChallengeMode && !isWeakRevision ? `${accent.bg} ${accent.border} text-white shadow-lg` : 'bg-white/5 border-white/5 opacity-50'}`}
                      >
                        {item.label}
                      </button>
                    ))}
                  </div>
                </div>

                <button 
                  onClick={() => {
                    setIsChallengeMode(true);
                    setMode('quiz');
                    setIsWeakRevision(false);
                    setIsSidebarOpen(false);
                    setShowResults(false);
                  }}
                  className={`flex items-center gap-3 p-4 rounded-3xl transition-all ${isChallengeMode ? `${accent.bg} text-white shadow-lg` : 'hover:bg-white/5 opacity-60 hover:opacity-100'}`}
                >
                  <Zap size={20} />
                  <div className="text-left">
                    <p className="text-xs font-black uppercase tracking-widest">75 Day Plan</p>
                    <p className="text-[9px] opacity-70">Progress: {Math.round((completedChallengeDays.length / TOTAL_PLAN_DAYS) * 100)}%</p>
                  </div>
                </button>

                <button 
                  onClick={() => {
                    setIsWeakRevision(true);
                    setIsSidebarOpen(false);
                    setShowResults(false);
                    setIsChallengeMode(false);
                  }}
                  className={`flex items-center gap-3 p-4 rounded-3xl transition-all ${isWeakRevision ? `bg-rose-600 text-white shadow-lg` : 'hover:bg-rose-500/5 text-rose-500/60 hover:text-rose-500'}`}
                >
                  <Award size={20} />
                  <div className="text-left">
                    <p className="text-xs font-black uppercase tracking-widest">Weak List</p>
                    <p className="text-[9px] opacity-70">{weakList.ow.length + weakList.sy.length + weakList.id.length + weakList.pv.length} terms to revise</p>
                  </div>
                </button>
              </div>

              <div className="mt-auto pt-6 border-t border-white/5 space-y-4">
                <button 
                  onClick={() => {
                    setIsSidebarOpen(false);
                    setIsSettingsOpen(true);
                  }}
                  className="w-full flex items-center gap-3 p-4 rounded-3xl hover:bg-white/5 transition-all opacity-60 hover:opacity-100"
                >
                  <Sun size={20} />
                  <p className="text-xs font-black uppercase tracking-widest">App Settings</p>
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Settings Modal */}
      <AnimatePresence>
        {isSettingsOpen && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsSettingsOpen(false)}
              className="absolute inset-0 bg-black/80 backdrop-blur-md"
            />
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className={`w-full max-w-sm rounded-[3rem] p-8 border space-y-8 relative overflow-hidden ${theme.card} ${theme.border}`}
            >
              <div className="flex justify-between items-center mb-2">
                <h3 className={`text-xl font-black ${accent.text} italic`}>SETTINGS</h3>
                <button onClick={() => setIsSettingsOpen(false)} className="opacity-40 hover:opacity-100 p-2"><X size={20} /></button>
              </div>

              <div className="space-y-6">
                <div className="space-y-3">
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] opacity-40">Accent Color</p>
                  <div className="flex flex-wrap gap-2">
                    {(Object.keys(ACCENTS) as AccentKey[]).map(ak => (
                      <button 
                        key={ak}
                        onClick={() => setAccentKey(ak)}
                        className={`w-10 h-10 rounded-full border-2 transition-all flex items-center justify-center ${accentKey === ak ? `border-white ring-2 ring-offset-2 ${ACCENTS[ak].dot.replace('bg-', 'ring-')}` : 'border-transparent shadow-sm'}`}
                      >
                        <div className={`w-6 h-6 rounded-full ${ACCENTS[ak].dot}`} />
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-3">
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] opacity-40">Appearance Themes</p>
                  <div className="grid grid-cols-2 gap-2">
                    {(Object.keys(THEMES) as ThemeKey[]).map(tk => (
                       <button 
                        key={tk}
                        onClick={() => setThemeKey(tk)}
                        className={`py-3 px-3 rounded-2xl text-[10px] font-bold uppercase tracking-tight border transition-all ${themeKey === tk ? `${accent.border} ${accent.bg.replace('bg-', 'bg-opacity-10 ' + accent.text)}` : 'border-white/5 opacity-60'}`}
                       >
                         {tk}
                       </button>
                    ))}
                  </div>
                </div>

                <div className="h-px bg-white/5" />

                <div className="grid grid-cols-2 gap-3">
                  <button 
                    onClick={() => {
                      const data = localStorage.getItem('jt_vocab_v2');
                      const blob = new Blob([data || '{}'], { type: 'application/json' });
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = `jt_vocab_backup_${new Date().toISOString().split('T')[0]}.json`;
                      a.click();
                    }}
                    className={`flex flex-col items-center gap-2 p-4 rounded-3xl border border-white/5 hover:bg-white/5 transition-all group`}
                  >
                    <Download size={20} className="group-hover:translate-y-0.5 transition-transform" />
                    <span className="text-[9px] font-black uppercase tracking-widest">Export</span>
                  </button>
                  <button 
                    onClick={() => {
                      const input = document.createElement('input');
                      input.type = 'file';
                      input.accept = '.json';
                      input.onchange = (e) => {
                        const file = (e.target as HTMLInputElement).files?.[0];
                        if (file) {
                          const reader = new FileReader();
                          reader.onload = (re) => {
                            try {
                              const parsed = JSON.parse(re.target?.result as string);
                              localStorage.setItem('jt_vocab_v2', JSON.stringify(parsed));
                              window.location.reload();
                            } catch (error) {
                              alert('Invalid backup file');
                            }
                          };
                          reader.readAsText(file);
                        }
                      };
                      input.click();
                    }}
                    className={`flex flex-col items-center gap-2 p-4 rounded-3xl border border-white/5 hover:bg-white/5 transition-all group`}
                  >
                    <Upload size={20} className="group-hover:-translate-y-0.5 transition-transform" />
                    <span className="text-[9px] font-black uppercase tracking-widest">Import</span>
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Main Header */}
      <header className={`w-full flex justify-between items-center p-4 md:p-6 sticky top-0 z-40 backdrop-blur-md border-b transition-colors ${theme.nav} ${theme.border}`}>
        <button 
          onClick={() => setIsSidebarOpen(true)}
          className={`p-2 rounded-xl transition-colors ${theme.secondary} hover:opacity-80`}
        >
          <Menu size={24} />
        </button>
        <div className="flex flex-col items-center">
          <h1 className="text-lg font-black tracking-tighter italic uppercase leading-none">
            {isWeakRevision ? <span className="text-rose-500">REVISING WEAK LIST</span> : 
             isChallengeMode ? <span className={accent.text}>75 DAYS CHALLENGE</span> :
             'JT VOCAB QUIZ'}
          </h1>
          <div className="flex items-center gap-2 mt-1.5 w-32">
            <div className="flex-1 h-1 bg-black/10 rounded-full overflow-hidden">
              <motion.div 
                initial={{ width: 0 }}
                animate={{ width: `${(stats.mastered / (stats.total || 1)) * 100}%` }}
                transition={{ duration: 1, ease: "easeOut" }}
                className={`h-full ${accent.bg}`} 
              />
            </div>
            <span className={`text-[8px] font-black ${accent.text}`}>
              {`${Math.round((stats.mastered / (stats.total || 1)) * 100)}%`}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <AnimatePresence mode="wait">
            {ACHIEVEMENTS.filter(a => a.unlocked).slice(-1).map(ach => (
              <motion.div
                key={ach.id}
                initial={{ x: 20, opacity: 0, scale: 0.8 }}
                animate={{ x: 0, opacity: 1, scale: 1 }}
                exit={{ x: -20, opacity: 0, scale: 0.8 }}
                transition={{ type: 'spring', damping: 15 }}
                className={`relative group`}
              >
                <div className={`flex items-center justify-center w-10 h-10 rounded-xl border bg-white/5 backdrop-blur-md shadow-lg ${accent.border}`}>
                  <div className="scale-110 drop-shadow-md">{ach.icon}</div>
                </div>
                <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                   <span className="text-[7px] font-black bg-black/80 px-1 rounded uppercase tracking-tighter">{ach.title}</span>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
          <div className="flex flex-col items-end">
            <span className={`text-[9px] font-black uppercase tracking-widest ${accent.text}`}>Streak 🔥</span>
            <span className="text-lg font-black leading-none">{streak}</span>
          </div>
        </div>
      </header>

      {/* Achievement Rail (Keep it as a secondary view if needed, but primary badge is in header) */}
      <main className="w-full max-w-2xl px-4 pt-6 pb-40 mx-auto">
        <motion.div
          drag="x"
          dragConstraints={{ left: 0, right: 0 }}
          onDragEnd={(_, info) => {
            const threshold = 50;
            if (info.offset.x > threshold) {
              // Swipe Right -> Prev Category
              const cats: Category[] = ['ow', 'sy', 'id', 'pv'];
              const idx = cats.indexOf(cat);
              const prev = cats[(idx - 1 + cats.length) % cats.length];
              setCat(prev);
              setAnswered({});
              setShowResults(false);
            } else if (info.offset.x < -threshold) {
              // Swipe Left -> Next Category
              const cats: Category[] = ['ow', 'sy', 'id', 'pv'];
              const idx = cats.indexOf(cat);
              const next = cats[(idx + 1) % cats.length];
              setCat(next);
              setAnswered({});
              setShowResults(false);
            }
          }}
          className="w-full"
        >
        {/* Achievements Rail */}
        {achievements.length > 0 && (
          <div className="mb-6 overflow-x-auto no-scrollbar pb-2">
            <div className="flex gap-3">
              {ACHIEVEMENTS.filter(a => a.unlocked).map(ach => (
                <motion.div 
                  key={ach.id}
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  className={`flex-shrink-0 flex items-center gap-2 px-4 py-2 rounded-2xl border ${theme.card} ${theme.border} bg-white/5 backdrop-blur-sm`}
                >
                  <span className="text-xl">{ach.icon}</span>
                  <div className="flex flex-col">
                    <span className="text-[8px] font-black uppercase tracking-tighter opacity-40 leading-none">Unlocked</span>
                    <span className="text-[10px] font-bold tracking-tight leading-none">{ach.title}</span>
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        )}

        {/* Floating Session Progress */}
        {!showResults && activeBatch.length > 0 && (
          <div className="fixed bottom-6 left-4 right-4 z-40 flex justify-center pointer-events-none">
            <motion.div 
              initial={{ y: 50, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              className={`pointer-events-auto px-6 py-3 rounded-full border shadow-2xl backdrop-blur-xl flex items-center gap-4 ${theme.card} ${theme.border} min-w-[280px]`}
            >
              <div className="flex-1">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-[9px] font-black uppercase tracking-[0.2em] opacity-40">Session mastery</span>
                  <span className={`text-[9px] font-black uppercase tracking-[0.2em] ${accent.text}`}>
                    {Object.keys(answered).length}/{activeBatch.length}
                  </span>
                </div>
                <div className="w-full h-1.5 bg-black/20 rounded-full overflow-hidden">
                  <motion.div 
                    initial={{ width: 0 }}
                    animate={{ width: `${(Object.keys(answered).length / activeBatch.length) * 100}%` }}
                    className={`h-full rounded-full ${accent.bg}`}
                  />
                </div>
              </div>
              <div className="h-8 w-px bg-white/10" />
              <div className="flex flex-col items-center">
                <span className="text-[9px] font-black uppercase tracking-tighter opacity-40">Score</span>
                <span className={`text-sm font-black ${accent.text}`}>
                  {currentScore}
                </span>
              </div>
            </motion.div>
          </div>
        )}
        {/* Mode Toggles */}
        {!isWeakRevision && (
          <div className={`flex p-1 rounded-2xl mb-6 shadow-inner ${theme.secondary}`}>
            <button 
              onClick={() => { setMode('quiz'); setAnswered({}); setShowResults(false); }}
              className={`flex-1 py-3 font-black text-[10px] tracking-widest rounded-xl transition-all ${mode === 'quiz' ? `${accent.bg} text-white shadow-lg` : 'opacity-40 text-slate-500'}`}
            >
              QUIZ MODE
            </button>
            <button 
              onClick={() => {setMode('learn'); setShowResults(false); }}
              className={`flex-1 py-3 font-black text-[10px] tracking-widest rounded-xl transition-all ${mode === 'learn' ? 'bg-slate-700 text-white' : 'opacity-40 text-slate-500'}`}
            >
              LEARN MODE
            </button>
          </div>
        )}

        {/* Day Strategy */}
        {isChallengeMode && (
          <div className={`mb-6 p-5 rounded-[2.5rem] border ${theme.border} ${theme.secondary} shadow-xl shadow-black/10 backdrop-blur-md relative overflow-hidden`}>
            <div className="absolute top-0 right-0 p-4 opacity-10">
              <Zap size={64} />
            </div>
            <p className="text-[10px] font-black uppercase tracking-[0.3em] mb-2 opacity-50 flex items-center gap-2">
              <Star size={10} fill="currentColor" /> Day {challengeDay} Strategy
            </p>
            <div className="space-y-3 relative z-10">
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <p className="text-lg font-black italic tracking-tighter leading-tight">
                    Daily goal: Master <span className={accent.text}>82 high-yield terms</span> to win the 75-day race.
                  </p>
                  <button 
                    onClick={() => {
                      const newDay = prompt("Jump to Day (1-75):", challengeDay.toString());
                      if (newDay && !isNaN(parseInt(newDay))) {
                        const d = Math.min(75, Math.max(1, parseInt(newDay)));
                        setChallengeDay(d);
                        setAnswered({});
                        setShowResults(false);
                      }
                    }}
                    className={`mt-2 text-[8px] font-black uppercase tracking-widest px-2 py-0.5 rounded border border-white/20 hover:bg-white/10 transition-colors ${accent.text}`}
                  >
                    Edit current day
                  </button>
                </div>
                <button 
                  onClick={() => setIsChallengeMode(false)}
                  className="px-3 py-1 rounded-full bg-black/30 text-[9px] font-black uppercase tracking-widest hover:bg-black/50 transition-colors"
                >
                  Exit Challenge
                </button>
              </div>
              <div className="flex gap-2 text-[9px] font-bold uppercase tracking-wider opacity-60">
                <span className="bg-black/20 px-2 py-0.5 rounded">OWS: 27</span>
                <span className="bg-black/20 px-2 py-0.5 rounded">SY: 24</span>
                <span className="bg-black/20 px-2 py-0.5 rounded">ID: 24</span>
                <span className="bg-black/20 px-2 py-0.5 rounded">PV: 7</span>
              </div>
              <div className="w-full bg-black/10 h-1.5 rounded-full mt-4 overflow-hidden">
                <motion.div 
                  initial={{ width: 0 }}
                  animate={{ width: `${(completedChallengeDays.length / TOTAL_PLAN_DAYS) * 100}%` }}
                  transition={{ duration: 1.5, ease: "backOut" }}
                  className={`h-full rounded-full ${accent.bg}`}
                />
              </div>
              <p className="text-[9px] font-bold uppercase tracking-widest opacity-40 text-right">
                Plan Completion: {Math.round((completedChallengeDays.length / TOTAL_PLAN_DAYS) * 100)}%
              </p>
            </div>
          </div>
        )}

        {!isWeakRevision && (
          <div className="flex flex-col gap-4 mb-8">
            <div className={`flex p-1.5 rounded-2xl shadow-inner ${theme.secondary} border ${theme.border}`}>
              {[
                { id: 'ow', label: 'OWS' },
                { id: 'sy', label: 'Synonyms' },
                { id: 'id', label: 'Idioms' },
                { id: 'pv', label: 'Phrasal' }
              ].map(item => (
                <button 
                  key={item.id}
                  onClick={() => { 
                    setCat(item.id as Category); 
                    setAnswered({}); 
                    setShowResults(false);
                    window.scrollTo(0, 0);
                  }}
                  className={`flex-1 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all ${cat === item.id ? `${accent.bg} text-white shadow-lg` : 'opacity-40 hover:opacity-100'}`}
                >
                  {item.label}
                </button>
              ))}
            </div>

            <div className="flex gap-2 overflow-x-auto pb-2 no-scrollbar">
              {isChallengeMode ? (
              Array.from({ length: TOTAL_PLAN_DAYS }).map((_, i) => {
                const day = i + 1;
                const isCompleted = completedChallengeDays.includes(day);
                return (
                  <button
                    key={i}
                    onClick={() => { setChallengeDay(day); setAnswered({}); setShowResults(false); }}
                    className={`px-6 py-2 rounded-xl font-black text-[10px] tracking-widest whitespace-nowrap transition-all border flex items-center gap-2 ${
                      challengeDay === day 
                        ? `${accent.bg} ${accent.border} text-white shadow-lg ${accent.shadow}` 
                        : isCompleted 
                          ? 'bg-emerald-500/20 border-emerald-500/50 text-emerald-500'
                          : `${theme.secondary} ${theme.border} opacity-50`
                    }`}
                  >
                    DAY {day}
                    {isCompleted && <CheckCircle2 size={12} className={challengeDay === day ? 'text-white' : 'text-emerald-500'} />}
                  </button>
                );
              })
            ) : (
              Array.from({ length: totalSets }).map((_, i) => {
                const isCompleted = completedSets[cat].includes(i);
                return (
                  <button
                    key={i}
                    onClick={() => { setCurSet(i); setAnswered({}); setShowResults(false); }}
                    className={`px-6 py-2 rounded-xl font-black text-[10px] tracking-widest whitespace-nowrap transition-all border flex items-center gap-2 ${
                      curSet === i 
                        ? `${accent.bg} ${accent.border} text-white shadow-lg ${accent.shadow}` 
                        : isCompleted
                          ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-500/80'
                          : `${theme.secondary} ${theme.border} opacity-50`
                    }`}
                  >
                    SET {i + 1}
                    {isCompleted && <CheckCircle2 size={12} className={curSet === i ? 'text-white' : 'text-emerald-500'} />}
                  </button>
                );
              })
            )}
            </div>
          </div>
        )}

        {mode === 'learn' && !isWeakRevision && !isChallengeMode ? (
          <div className="flex flex-col gap-4 mt-2">
            {activeBatch.map((item, idx) => {
              const globalSerial = idx + 1 + (curSet * SET_SIZE);
              return (
                <LearnCard 
                  key={`${item.id}-${idx}`}
                  item={item}
                  cat={cat}
                  globalSerial={globalSerial}
                  theme={theme}
                  accent={accent}
                />
              );
            })}
            {activeBatch.length > 0 && (
               <button 
                 onClick={finishSession}
                 className={`w-full mt-10 py-5 text-white font-black text-lg rounded-3xl shadow-xl active:scale-[0.98] transition-all tracking-tighter ${accent.bg} ${accent.shadow} hover:brightness-110`}
               >
                 MARK AS LEARNED
               </button>
            )}
            {activeBatch.length === 0 && <p className="text-center opacity-30 py-20 font-bold italic">No data found in this set.</p>}
          </div>
        ) : (
          /* Quiz View & Weak Revision View */
          <div className="mt-2 flex flex-col gap-6">
            {!showResults ? (
              <>
                {activeBatch.map((item, idx) => {
                  let itemCat: Category = cat;
                  if (item.id.startsWith('ow')) itemCat = 'ow';
                  else if (item.id.startsWith('sy')) itemCat = 'sy';
                  else if (item.id.startsWith('id')) itemCat = 'id';
                  else if (item.id.startsWith('pv')) itemCat = 'pv';

                  return (
                    <QuizCard 
                      key={`${item.id}-${idx}`}
                      item={item}
                      idx={idx}
                      cat={itemCat}
                      database={database}
                      answered={answered}
                      showHint={showHint}
                      onAnswer={handleAnswer}
                      onToggleHint={(i) => setShowHint(p => ({ ...p, [i]: !p[i] }))}
                      theme={theme}
                      themeKey={themeKey}
                      accent={accent}
                      globalSerial={isWeakRevision ? idx + 1 : idx + 1 + (curSet * SET_SIZE)}
                    />
                  );
                })}

                {activeBatch.length > 0 ? (
                  <button 
                    onClick={finishSession}
                    className={`w-full mt-10 py-5 text-white font-black text-lg rounded-3xl shadow-xl active:scale-[0.98] transition-all tracking-tighter ${accent.bg} ${accent.shadow} hover:brightness-110`}
                  >
                    FINISH {isWeakRevision ? 'REVISION' : isChallengeMode ? `DAY ${challengeDay} ${cat.toUpperCase()}` : `SET ${curSet + 1}`}
                  </button>
                ) : (
                  <div className={`p-16 rounded-[3rem] text-center border-2 border-dashed ${theme.border} opacity-40`}>
                    <p className="text-2xl font-black italic tracking-tighter mb-2">VOID SPACE</p>
                    <p className="text-xs font-bold uppercase tracking-widest">Nothing here to master yet</p>
                  </div>
                )}
                <div className="h-32 pointer-events-none" />
              </>
            ) : (
              <motion.div 
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className={`p-12 rounded-[3.5rem] text-center border transition-all shadow-2xl ${theme.card} ${theme.border}`}
              >
                <div className={`mb-6 flex justify-center ${accent.text}`}>
                  <Trophy size={72} strokeWidth={3} />
                </div>
                <div className={`inline-block px-4 py-1 rounded-full text-[10px] font-black tracking-[0.3em] mb-4 border ${accent.border} ${accent.text}`}>
                  RANK: {getRank(currentScore, activeBatch.length).label}
                </div>
                <h2 className="text-4xl font-black mb-2 italic tracking-tighter leading-none">
                  {currentScore === activeBatch.length ? 'GOD-LIKE!' : 'SESSION OVER'}
                </h2>
                <div className={`text-8xl font-black my-8 tracking-tighter ${accent.text}`}>
                   {currentScore}/{activeBatch.length}
                </div>
                <p className="opacity-40 font-bold uppercase tracking-widest text-[10px] mb-12">Cognitive Synchronization Complete</p>
                
                <div className="grid grid-cols-1 gap-3">
                  <button 
                    onClick={() => { setAnswered({}); setShowResults(false); }}
                    className={`py-5 text-white font-black rounded-3xl transition-colors shadow-lg tracking-wide ${accent.bg} ${accent.shadow} hover:brightness-110`}
                  >
                    CONTINUE PRACTICE
                  </button>
                  <button 
                    onClick={() => {
                      if (isWeakRevision) {
                         setIsWeakRevision(false);
                      } else if (isChallengeMode) {
                        const cats: Category[] = ['ow', 'sy', 'id', 'pv'];
                        const idx = cats.indexOf(cat);
                        if (idx < cats.length - 1) {
                          setCat(cats[idx + 1]);
                        } else {
                          if (challengeDay < TOTAL_PLAN_DAYS) setChallengeDay(challengeDay + 1);
                          setCat('ow');
                        }
                      } else {
                        if (curSet < totalSets - 1) setCurSet(curSet + 1);
                      }
                      setAnswered({});
                      setShowResults(false);
                      window.scrollTo(0,0);
                    }}
                    className={`py-5 font-black rounded-3xl transition-colors tracking-wide ${theme.secondary}`}
                  >
                    {isWeakRevision ? 'BACK TO MAIN' : isChallengeMode ? 'NEXT TASK' : 'PROCEED TO NEXT'}
                  </button>
                </div>
              </motion.div>
            )}
          </div>
        )}
        </motion.div>
      </main>

      <div className="h-40 pointer-events-none" /> {/* Extra spacing for mobile devices */}

      <style>{`
        .no-scrollbar::-webkit-scrollbar { display: none; }
        .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
        @import url('https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&family=Plus+Jakarta+Sans:wght@200;400;600;800&display=swap');
        body { font-family: 'Plus Jakarta Sans', sans-serif; }
        .font-serif { font-family: 'Instrument Serif', serif; }
      `}</style>
    </div>
  );
}
