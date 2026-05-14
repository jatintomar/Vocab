import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Sparkles, BrainCircuit, CheckCircle2, XCircle, Info, ChevronRight, LayoutGrid, Clock, Trophy, BookOpen } from 'lucide-react';
import { DailyComprehensionData, PQRSQuestion } from '../services/comprehensionService';

interface ComprehensionViewProps {
  data: DailyComprehensionData;
  theme: any;
  accent: any;
  onFinish: (results: any) => void;
  sessionMode: 'quiz' | 'exam';
}

export const ComprehensionView: React.FC<ComprehensionViewProps> = ({ data, theme, accent, onFinish, sessionMode }) => {
  const [step, setStep] = useState<'pqrs' | 'cloze' | 'rc'>('pqrs');
  const [pqrsIdx, setPqrsIdx] = useState(0);
  const [clozeIdx, setClozeIdx] = useState(0);
  const [rcIdx, setRcIdx] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [showExplanation, setShowExplanation] = useState<string | null>(null);
  const [isFinished, setIsFinished] = useState(false);

  const handleAnswer = (qid: string, val: string) => {
    setAnswers(prev => ({ ...prev, [qid]: val }));
  };

  const currentPqrs = data.pqrs[pqrsIdx];

  const renderPQRS = () => (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-6">
      <div className="flex justify-between items-center">
        <span className={`text-[10px] font-black uppercase tracking-widest ${accent.text}`}>PQRS Jumble {pqrsIdx + 1}/5</span>
        <div className="flex gap-1 h-1 bg-white/5 rounded-full overflow-hidden w-24">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: `${((pqrsIdx + 1) / data.pqrs.length) * 100}%` }}
            transition={{ duration: 0.5 }}
            className={`h-full ${accent.bg}`} 
          />
        </div>
      </div>

      <div className={`p-6 rounded-[2.5rem] border ${theme.card} ${theme.border} space-y-4 shadow-xl`}>
        <div className="space-y-3">
          {currentPqrs.s1 && (
            <div className="p-3 rounded-2xl bg-black/10 border border-white/5">
              <p className="text-[10px] font-black uppercase tracking-widest opacity-40 mb-1">Fixed Start (S1)</p>
              <p className="text-sm font-medium leading-relaxed">{currentPqrs.s1}</p>
            </div>
          )}
          
          <div className="space-y-2 py-2">
            {currentPqrs.sentences.map((s, i) => (
              <div key={i} className="flex gap-4 items-start group">
                <span className={`mt-1 flex-shrink-0 w-6 h-6 rounded-lg flex items-center justify-center text-[10px] font-black ${accent.bg} text-white`}>
                  {['P', 'Q', 'R', 'S'][i]}
                </span>
                <p className="text-sm font-medium leading-relaxed opacity-90 group-hover:opacity-100 transition-opacity">{s}</p>
              </div>
            ))}
          </div>

          {currentPqrs.s6 && (
            <div className="p-3 rounded-2xl bg-black/10 border border-white/5">
              <p className="text-[10px] font-black uppercase tracking-widest opacity-40 mb-1">Fixed End (S6)</p>
              <p className="text-sm font-medium leading-relaxed">{currentPqrs.s6}</p>
            </div>
          )}
        </div>

        <div className="pt-4 grid grid-cols-2 gap-3">
          {['PQRS', 'PSQR', 'QPSR', 'SPRQ', 'RQPS', 'QSRP'].slice(0, 4).concat(currentPqrs.correctSequence).sort().filter((v, i, a) => a.indexOf(v) === i).slice(0, 4).map(opt => {
            const isCorrect = opt === currentPqrs.correctSequence;
            const isSelected = answers[currentPqrs.id] === opt;
            const showResult = isSelected && (sessionMode === 'quiz');

            return (
              <button
                key={opt}
                onClick={() => handleAnswer(currentPqrs.id, opt)}
                className={`py-4 rounded-2xl border text-xs font-black transition-all ${
                  isSelected 
                    ? (sessionMode === 'quiz' ? (isCorrect ? 'bg-emerald-500 border-emerald-400' : 'bg-rose-500 border-rose-400') : `${accent.bg} border-white/40`) 
                    : `${theme.secondary} border-white/5 opacity-80 hover:opacity-100`
                }`}
              >
                {opt}
              </button>
            );
          })}
        </div>

        {sessionMode === 'quiz' && answers[currentPqrs.id] && (
          <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} className="space-y-3 pt-4 border-t border-white/5">
            <button 
              onClick={() => setShowExplanation(showExplanation === currentPqrs.id ? null : currentPqrs.id)}
              className={`flex items-center gap-2 text-[9px] font-black uppercase tracking-widest ${accent.text}`}
            >
              <Sparkles size={12} /> {showExplanation === currentPqrs.id ? 'Hide AI Logic' : 'Why this sequence?'}
            </button>
            {showExplanation === currentPqrs.id && (
              <p className="text-[11px] leading-relaxed opacity-70 italic bg-black/10 p-4 rounded-2xl">
                {currentPqrs.explanation}
              </p>
            )}
          </motion.div>
        )}
      </div>

      <button
        onClick={() => {
          if (pqrsIdx < 4) setPqrsIdx(p => p + 1);
          else {
            setStep('cloze');
            setClozeIdx(0);
          }
          window.scrollTo(0, 0);
        }}
        disabled={!answers[currentPqrs.id]}
        className={`w-full py-5 rounded-3xl font-black text-sm tracking-widest flex items-center justify-center gap-2 ${answers[currentPqrs.id] ? accent.bg : 'bg-white/10 opacity-30 cursor-not-allowed'}`}
      >
        {pqrsIdx < 4 ? 'NEXT JUMBLE' : 'PROCEED TO CLOZE TESTS'} <ChevronRight size={18} />
      </button>
    </motion.div>
  );

  const [clozeStep, setClozeStep] = useState(0); // Add this state to track blank index within a passage

  const renderCloze = () => {
    const currentCloze = data.cloze[clozeIdx];
    const currentBlank = currentCloze.blanks[clozeStep];
    const blankAid = `cloze_${currentCloze.id}_${currentBlank.index}`;

    return (
      <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-6">
        <div className="flex justify-between items-center text-[10px] font-black uppercase tracking-widest">
          <div className="flex flex-col">
            <span className={accent.text}>Section 2: Cloze Test {clozeIdx + 1}/{data.cloze.length}</span>
            <span className="opacity-40">Blank {clozeStep + 1}/{currentCloze.blanks.length}</span>
          </div>
          <div className="flex gap-1 h-1 bg-white/5 rounded-full overflow-hidden w-24">
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: `${((clozeStep + 1) / currentCloze.blanks.length) * 100}%` }}
              transition={{ duration: 0.5 }}
              className={`h-full ${accent.bg}`} 
            />
          </div>
        </div>

        <div className={`p-8 rounded-[3rem] border ${theme.card} ${theme.border} shadow-2xl space-y-8`}>
          <div className="p-6 rounded-3xl bg-black/5 border border-white/5">
             <p className="text-sm leading-[2.2] font-medium opacity-90 whitespace-pre-wrap">
               {/* Highlight current blank in passage */}
               {currentCloze.passage.split(new RegExp(`\\((${currentBlank.index})\\)`)).map((part, i) => {
                 if (part === String(currentBlank.index)) {
                   return <span key={i} className={`px-2 py-0.5 rounded mx-1 font-black ${accent.bg} text-white animate-pulse`}>({part})</span>;
                 }
                 return part;
               })}
             </p>
          </div>

          <div className="space-y-6">
            <div className="flex items-center gap-3">
               <div className={`w-8 h-8 rounded-xl ${accent.bg} flex items-center justify-center text-white font-black text-xs`}>
                  {currentBlank.index}
               </div>
               <p className="text-[10px] font-black uppercase tracking-widest opacity-40">Select the most appropriate word for blank ({currentBlank.index})</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {currentBlank.options.map(opt => {
                const isSelected = answers[blankAid] === opt;
                const isCorrect = opt === currentBlank.answer;
                
                return (
                  <button
                    key={opt}
                    onClick={() => handleAnswer(blankAid, opt)}
                    className={`text-left p-5 rounded-2xl border text-sm font-bold transition-all relative overflow-hidden group ${
                      isSelected 
                        ? (sessionMode === 'quiz' ? (isCorrect ? 'bg-emerald-500/20 border-emerald-500 text-emerald-500' : 'bg-rose-500/20 border-rose-500 text-rose-500') : `${accent.bg} border-white/40 text-white`) 
                        : `${theme.secondary} border-white/5 hover:bg-white/5`
                    }`}
                  >
                    {opt}
                    {isSelected && sessionMode === 'quiz' && (
                       <div className="absolute right-4 top-1/2 -translate-y-1/2">
                          {isCorrect ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
                       </div>
                    )}
                  </button>
                );
              })}
            </div>
            
            {sessionMode === 'quiz' && answers[blankAid] && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className={`p-4 rounded-2xl border ${theme.secondary} border-white/5`}>
                 <p className="text-[10px] font-black uppercase tracking-widest opacity-40 mb-1 flex items-center gap-2">
                    <Sparkles size={10} className={accent.text} /> logic insight
                 </p>
                 <p className="text-xs opacity-70 italic leading-relaxed">{currentBlank.explanation}</p>
              </motion.div>
            )}
          </div>
        </div>

        <button
          onClick={() => {
            if (clozeStep < currentCloze.blanks.length - 1) {
              setClozeStep(p => p + 1);
            } else {
              if (clozeIdx < data.cloze.length - 1) {
                setClozeIdx(p => p + 1);
                setClozeStep(0);
              } else {
                setStep('rc');
              }
            }
            window.scrollTo(0, 0);
          }}
          disabled={!answers[blankAid]}
          className={`w-full py-5 rounded-3xl font-black text-sm tracking-widest flex items-center justify-center gap-2 ${answers[blankAid] ? accent.bg : 'bg-white/10 opacity-30 cursor-not-allowed'} shadow-xl`}
        >
          {clozeStep < currentCloze.blanks.length - 1 
            ? 'NEXT BLANK' 
            : (clozeIdx < data.cloze.length - 1 ? 'NEXT CLOZE PASSAGE' : 'PROCEED TO READING PASSAGE')} <ChevronRight size={18} />
        </button>
      </motion.div>
    );
  };

const renderRC = () => {
    const q = data.rc.questions[rcIdx];
    const aid = `rc_${data.rc.id}_${rcIdx}`;
    
    return (
      <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="space-y-6 pb-20">
        <div className="flex justify-between items-center text-[10px] font-black uppercase tracking-widest">
          <div className="flex flex-col">
            <span className={accent.text}>Section 3: Reading Comprehension</span>
            <span className="opacity-40">Question {rcIdx + 1}/{data.rc.questions.length}</span>
          </div>
          <div className="flex gap-1 h-1 bg-white/5 rounded-full overflow-hidden w-24">
            <motion.div 
              initial={{ width: 0 }}
              animate={{ width: `${((rcIdx + 1) / data.rc.questions.length) * 100}%` }}
              transition={{ duration: 0.5 }}
              className={`h-full ${accent.bg}`} 
            />
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
          {/* Sticky/Fixed Passage Container */}
          <div className="lg:sticky lg:top-24 lg:max-h-[calc(100vh-140px)] flex flex-col gap-4">
            <div className={`p-6 md:p-8 rounded-[2.5rem] border ${theme.card} ${theme.border} shadow-xl overflow-hidden flex flex-col`}>
               <div className="flex justify-between items-center mb-4">
                  <span className={`text-[10px] font-black uppercase tracking-widest ${accent.text}`}>The Passage</span>
                  <BookOpen size={14} className="opacity-30" />
               </div>
               <div className="overflow-y-auto pr-2 no-scrollbar hover:no-scrollbar lg:max-h-full max-h-[300px]">
                  <p className="text-sm md:text-base leading-[2.2] font-medium opacity-90 italic-serif whitespace-pre-wrap">
                    {data.rc.passage}
                  </p>
               </div>
               <div className="mt-4 pt-4 border-t border-white/5 lg:hidden">
                  <p className="text-[9px] font-black uppercase tracking-widest opacity-30 text-center">Scroll above to read passage</p>
               </div>
            </div>
          </div>

          {/* Questions Area */}
          <div className="space-y-6">
            <div className={`p-6 md:p-8 rounded-[2.5rem] border ${theme.card} ${theme.border} shadow-xl space-y-4`}>
              <div className="flex items-center gap-2 mb-2">
                 <span className={`w-6 h-6 rounded-lg flex items-center justify-center text-[10px] font-black ${accent.bg} text-white`}>
                    {rcIdx + 1}
                 </span>
                 <h4 className="text-sm md:text-base font-black leading-tight italic">{q.text}</h4>
              </div>
              
              <div className="grid gap-2">
                {q.options.map(opt => {
                  const isSelected = answers[aid] === opt;
                  const isCorrect = opt === q.answer;
                  
                  return (
                    <button
                      key={opt}
                      onClick={() => handleAnswer(aid, opt)}
                      className={`text-left p-4 rounded-2xl border text-xs font-bold transition-all ${
                        isSelected 
                          ? (sessionMode === 'quiz' ? (isCorrect ? 'bg-emerald-500 border-emerald-400' : 'bg-rose-500 border-rose-400') : `${accent.bg} border-white/40`) 
                          : `${theme.secondary} border-white/5 hover:bg-white/5`
                      }`}
                    >
                      {opt}
                    </button>
                  );
                })}
              </div>
              {sessionMode === 'quiz' && answers[aid] && (
                <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} className="p-4 bg-black/10 rounded-2xl border border-white/5">
                   <p className="text-[10px] font-black uppercase tracking-widest opacity-40 mb-1">AI Logic Extraction</p>
                   <p className="text-xs opacity-70 leading-relaxed font-medium">{q.explanation}</p>
                </motion.div>
              )}
            </div>

            <button
              onClick={() => {
                if (rcIdx < data.rc.questions.length - 1) {
                  setRcIdx(p => p + 1);
                  window.scrollTo(0, 0);
                } else {
                  setIsFinished(true);
                  onFinish(answers);
                  window.scrollTo(0, 0);
                }
              }}
              disabled={!answers[aid]}
              className={`w-full py-5 rounded-3xl font-black text-sm tracking-widest flex items-center justify-center gap-2 ${answers[aid] ? accent.bg : 'bg-white/10 opacity-30 cursor-not-allowed'} shadow-2xl shadow-black hover:brightness-110 active:scale-[0.98] transition-all`}
            >
              {rcIdx < data.rc.questions.length - 1 ? 'NEXT QUESTION' : 'SUBMIT DAILY SESSION'} {rcIdx < data.rc.questions.length - 1 ? <ChevronRight size={18} /> : <Trophy size={18} />}
            </button>
          </div>
        </div>
      </motion.div>
    );
  };

  if (isFinished) {
    return (
      <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} className={`p-8 rounded-[3rem] border ${theme.card} ${theme.border} text-center space-y-8`}>
        <div className="flex justify-center">
          <Trophy size={64} className={accent.text} />
        </div>
        <div>
          <h2 className="text-3xl font-black italic italic-serif mb-2 uppercase">Session Completed</h2>
          <p className="opacity-40 text-[10px] font-black uppercase tracking-widest">Global Comprehension Status Synced</p>
        </div>
        <p className="text-sm font-medium opacity-80 leading-relaxed">
          You have successfully completed today's comprehension module based on the 2026 CGL/CHSL pattern.
        </p>
        <button
          onClick={() => window.location.reload()}
          className={`w-full py-4 rounded-2xl font-black text-sm tracking-widest ${accent.bg}`}
        >
          BACK TO DASHBOARD
        </button>
      </motion.div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      <motion.div
        drag="x"
        dragConstraints={{ left: 0, right: 0 }}
        onDragEnd={(_, info) => {
          const threshold = 50;
          if (info.offset.x > threshold) {
            // Swipe right -> Previous
            if (step === 'pqrs' && pqrsIdx > 0) setPqrsIdx(p => p - 1);
            else if (step === 'cloze') {
                if (clozeIdx > 0) setClozeIdx(p => p - 1);
                else setStep('pqrs');
            }
            else if (step === 'rc') {
                if (rcIdx > 0) setRcIdx(p => p - 1);
                else setStep('cloze');
            }
          } else if (info.offset.x < -threshold) {
            // Swipe left -> Next
            if (step === 'pqrs') {
                if (pqrsIdx < 4 && answers[currentPqrs.id]) setPqrsIdx(p => p + 1);
                else if (pqrsIdx === 4 && answers[currentPqrs.id]) setStep('cloze');
            } else if (step === 'cloze') {
                const currentCloze = data.cloze[clozeIdx];
                const allAnswered = currentCloze.blanks.every(b => answers[`cloze_${currentCloze.id}_${b.index}`]);
                if (allAnswered) {
                    if (clozeIdx < data.cloze.length - 1) setClozeIdx(p => p + 1);
                    else setStep('rc');
                }
            } else if (step === 'rc') {
                const aid = `rc_${data.rc.id}_${rcIdx}`;
                if (answers[aid] && rcIdx < data.rc.questions.length - 1) setRcIdx(p => p + 1);
            }
          }
        }}
      >
        {step === 'pqrs' && renderPQRS()}
        {step === 'cloze' && renderCloze()}
        {step === 'rc' && renderRC()}
      </motion.div>
      
      <div className="mt-8 flex justify-center items-center gap-4 opacity-40">
        <LayoutGrid size={16} />
        <span className="text-[8px] font-black uppercase tracking-[0.4em]">Integrated Logic Engine 2.0</span>
      </div>
    </div>
  );
};
