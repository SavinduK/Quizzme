import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Modal, Pressable, ScrollView, StatusBar, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import Header from './components/header';
import QuizCard from './components/quiz-card';
import { Colors } from './constants/theme';

const AI_CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;
const PAST_PAPERS_DIR = `${FileSystem.documentDirectory}pastpapers/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

type QuestionType = 'mcq' | 'tf' | 'sa' | 'seq';
type SourceMode = 'ai' | 'pastpaper';

export default function HomeFeed() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // Source selection state (AI vs Past Papers)
  const [sourceMode, setSourceMode] = useState<SourceMode>('ai');

  const [subjects, setSubjects] = useState<string[]>([]);
  const [terms, setTerms] = useState<string[]>([]);
  
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  const [selectedType, setSelectedType] = useState<QuestionType>('mcq');

  const [compiledPool, setCompiledPool] = useState<any[]>([]);
  const [quizFinished, setQuizFinished] = useState<boolean>(false);
  const [maxQuestionsCount, setMaxQuestionsCount] = useState<number>(5);
  
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [runningScore, setRunningScore] = useState<number>(0);
  const [chosenAnswer, setChosenAnswer] = useState<string | null>(null);
  
  // True / False State
  const [tfSelections, setTfSelections] = useState<{ [key: number]: boolean | null }>({ 0: null, 1: null, 2: null, 3: null, 4: null });
  const [tfChecked, setTfChecked] = useState<boolean>(false);
  const [tfQuestionScore, setTfQuestionScore] = useState<number>(0);

  // Short Answer State
  const [saInputText, setSaInputText] = useState<string>('');
  const [saChecked, setSaChecked] = useState<boolean>(false);

  // SEQ State
  const [showSeqAnswer, setShowSeqAnswer] = useState<boolean>(false);
  const [seqUserNotes, setSeqUserNotes] = useState<string>('');

  // Unified Bottom Drawer Filter Panel Visibility Toggle
  const [filterPanelVisible, setFilterPanelVisible] = useState(false);

  const getTargetDirectory = (mode: SourceMode) => {
    return mode === 'pastpaper' ? PAST_PAPERS_DIR : AI_CACHE_DIR;
  };

  const fetchAvailableDecks = async () => {
    try {
      const keyFileInfo = await FileSystem.getInfoAsync(KEY_FILE_URI);
      if (keyFileInfo.exists) {
        const keyFileContent = await FileSystem.readAsStringAsync(KEY_FILE_URI);
        if (keyFileContent) {
          const lines = keyFileContent.split(/\r?\n/);
          if (lines.length >= 2) {
            const parsedCount = parseInt(lines[1].trim(), 10);
            if (!isNaN(parsedCount) && parsedCount > 0) {
              setMaxQuestionsCount(parsedCount);
            }
          }
        }
      }

      const activeDir = getTargetDirectory(sourceMode);
      const folderInfo = await FileSystem.getInfoAsync(activeDir);
      if (!folderInfo.exists) {
        setSubjects([]);
        setTerms([]);
        return;
      }

      const files = await FileSystem.readDirectoryAsync(activeDir);
      const uniqueSubjects = new Set<string>();
      const uniqueTerms = new Set<string>();

      for (const file of files) {
        if (file.endsWith('.json') || file.endsWith('.txt')) {
          const cleanName = file.replace(/\.(json|txt)$/, '');
          const parts = cleanName.split('-');
          
          if (parts.length >= 3) {
            // Handle prefix stripping if present (e.g. mcq_cardio-physio-term1)
            const subjectPart = parts[1].includes('_') ? parts[1].split('_').slice(1).join('_') : parts[1];
            const parsedSubject = subjectPart.replace(/_/g, ' ');
            const parsedTerm = parts[2].replace(/_/g, ' ');
            
            uniqueSubjects.add(parsedSubject);
            uniqueTerms.add(parsedTerm);
          }
        }
      }
      setSubjects(Array.from(uniqueSubjects).sort());
      setTerms(Array.from(uniqueTerms).sort());
    } catch (e) { 
      console.error("Error reading data file systems:", e); 
    }
  };

  const generateRandomSession = async () => {
    try {
      const activeDir = getTargetDirectory(sourceMode);
      const folderInfo = await FileSystem.getInfoAsync(activeDir);
      if (!folderInfo.exists) {
        setCompiledPool([]);
        return;
      }

      const files = await FileSystem.readDirectoryAsync(activeDir);
      let rawQuestions: any[] = [];

      for (const file of files) {
        if (!file.endsWith('.json') && !file.endsWith('.txt')) continue;
        
        const cleanName = file.replace(/\.(json|txt)$/, '');
        const parts = cleanName.split('-');
        
        if (parts.length >= 3) {
          const subjectPart = parts[1].includes('_') ? parts[1].split('_').slice(1).join('_') : parts[1];
          const fileSubject = subjectPart.replace(/_/g, ' ');
          const fileTerm = parts[2].replace(/_/g, ' ');

          const matchSubject = !selectedSubject || fileSubject.toLowerCase() === selectedSubject.toLowerCase();
          const matchTerm = !selectedTerm || fileTerm.toLowerCase() === selectedTerm.toLowerCase();

          if (matchSubject && matchTerm) {
            const rawContent = await FileSystem.readAsStringAsync(`${activeDir}${file}`);
            if (!rawContent || rawContent.trim() === '') continue;

            try {
              const parsed = JSON.parse(rawContent);
              let questionsList: any[] = [];
              
              if (Array.isArray(parsed)) {
                questionsList = parsed;
              } else if (parsed && Array.isArray(parsed.questions)) {
                questionsList = parsed.questions;
              }

              questionsList = questionsList.filter((q) => {
                let qType = '';

                if (Array.isArray(q.sub_questions)) {
                  qType = 'seq';
                } else if (Array.isArray(q.statements) && Array.isArray(q.answers)) {
                  qType = 'tf';
                } else if (Array.isArray(q.options) && typeof q.correct_answer === 'string') {
                  qType = 'mcq';
                } else if (q.explanation && typeof q.correct_answer === 'string') {
                  qType = 'sa';
                } else if (q.type) {
                  qType = q.type.toLowerCase();
                }

                return qType === selectedType.toLowerCase();
              });

              rawQuestions.push(...questionsList);
            } catch (err) {
              console.error(`Error parsing JSON in ${file}:`, err);
            }
          }
        }
      }

      setCompiledPool(rawQuestions.sort(() => 0.5 - Math.random()).slice(0, maxQuestionsCount));
      resetQuizSessionState();
    } catch (e) {
      console.error('Error generating random session:', e);
    }
  };

  const resetQuizSessionState = () => {
    setCurrentQuestionIndex(0);
    setRunningScore(0);
    setQuizFinished(false);
    setChosenAnswer(null);
    setTfSelections({ 0: null, 1: null, 2: null, 3: null, 4: null });
    setTfChecked(false);
    setTfQuestionScore(0);
    setSaInputText('');
    setSaChecked(false);
    setShowSeqAnswer(false);
    setSeqUserNotes('');
  };

  useFocusEffect(useCallback(() => { fetchAvailableDecks(); }, [sourceMode]));
  useFocusEffect(useCallback(() => { generateRandomSession(); }, [sourceMode, selectedSubject, selectedTerm, selectedType, maxQuestionsCount]));

  const evaluateTfQuestion = () => {
    if (compiledPool.length === 0) return;
    const currentQ = compiledPool[currentQuestionIndex];
    let calculatedQScore = 0;

    for (let i = 0; i < 5; i++) {
      const selected = tfSelections[i];
      const realAnswer = currentQ.answers?.[i];
      if (selected !== null && selected !== undefined) {
        if (selected === realAnswer) {
          calculatedQScore += 1;
        } else {
          calculatedQScore -= 1;
        }
      }
    }

    const finalClampedQScore = Math.max(0, calculatedQScore);
    setTfQuestionScore(finalClampedQScore);
    setRunningScore(p => p + finalClampedQScore);
    setTfChecked(true);
  };

  const handleNextQuestion = () => {
    if (currentQuestionIndex + 1 < compiledPool.length) {
      setCurrentQuestionIndex(prev => prev + 1);
      setChosenAnswer(null);
      setTfSelections({ 0: null, 1: null, 2: null, 3: null, 4: null });
      setTfChecked(false);
      setTfQuestionScore(0);
      setSaInputText('');
      setSaChecked(false);
      setShowSeqAnswer(false);
      setSeqUserNotes('');
    } else {
      setQuizFinished(true);
    }
  };

  const resetFilters = () => {
    setSelectedSubject(null);
    setSelectedTerm(null);
    setSelectedType('mcq');
  };

  const hasActiveFilters = selectedSubject || selectedTerm || selectedType !== 'mcq' || sourceMode !== 'ai';

  const maxPossibleScore = selectedType === 'tf' ? compiledPool.length * 5 : compiledPool.length;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      <Header title="Questions" onRightButtonPress={() => router.push('/add-papers')} />

      <View style={styles.controlRow}>
        <View style={styles.summaryTextContainer}>
          <Text style={[styles.summaryLabel, { color: theme.subtext }]}>
            {sourceMode === 'pastpaper' ? '📄 Saved Past Papers' : '⚡ AI Generated Questions'}
          </Text>
        </View>

        <Pressable
          onPress={() => setFilterPanelVisible(true)}
          style={[
            styles.filterActionButton,
            { backgroundColor: theme.background, borderColor: hasActiveFilters ? theme.accent : theme.border },
          ]}
        >
          <FontAwesome5 name="sliders-h" size={15} color={hasActiveFilters ? theme.accent : theme.title} />
        </Pressable>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {compiledPool.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name={sourceMode === 'pastpaper' ? "file-alt" : "graduation-cap"} size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>
              {sourceMode === 'pastpaper' 
                ? "No saved past paper questions match your selection." 
                : "No AI generated questions found for this criteria."}
            </Text>
            {hasActiveFilters && (
              <Pressable style={[styles.inlineClearBtn, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={resetFilters}>
                <Text style={{ color: theme.accent, fontWeight: '600', fontSize: 13 }}>Reset Filters</Text>
              </Pressable>
            )}
          </View>
        ) : quizFinished ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="check-double" size={50} color="#4ade80" />
            <Text style={[styles.emptyText, { color: theme.title, fontSize: 18, fontWeight: '700' }]}>Quiz Finished!</Text>
            
            <View style={[styles.scoreContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <Text style={[styles.scoreText, { color: theme.title }]}>
                Final Score: <Text style={{ color: theme.accent }}>{runningScore}</Text> / {maxPossibleScore}
              </Text>
            </View>

            <Pressable 
              style={[styles.refreshButton, { backgroundColor: theme.card, borderColor: theme.accent }]}
              onPress={generateRandomSession}
            >
              <FontAwesome5 name="redo" size={14} color={theme.accent} style={{ marginRight: 8 }} />
              <Text style={[styles.refreshButtonText, { color: theme.accent }]}>Start New Session</Text>
            </Pressable>
          </View>
        ) : (
          <QuizCard
            item={compiledPool[currentQuestionIndex]}
            chosenAnswer={chosenAnswer}
            runningScore={runningScore}
            maxPossibleScore={maxPossibleScore}
            setChosenAnswer={(answer) => setChosenAnswer(answer)}
            setRunningScore={setRunningScore}
            tfSelections={tfSelections}
            setTfSelections={setTfSelections}
            tfChecked={tfChecked}
            tfQuestionScore={tfQuestionScore}
            evaluateTfQuestion={evaluateTfQuestion}
            handleNextQuestion={handleNextQuestion}
            currentQuestionIdx={currentQuestionIndex}
            totalQuestions={compiledPool.length}
            
            // Short Answer bindings
            isSAQuiz={selectedType === 'sa'}
            saInputText={saInputText}
            setSaInputText={setSaInputText}
            saChecked={saChecked}
            setSaChecked={setSaChecked}

            // SEQ bindings
            isSEQQuiz={selectedType === 'seq'}
            showSeqAnswer={showSeqAnswer}
            setShowSeqAnswer={setShowSeqAnswer}
            seqUserNotes={seqUserNotes}
            setSeqUserNotes={setSeqUserNotes}
          />
        )}
      </ScrollView>

      {/* --- INTEGRATED BOTTOM SHEET FILTER MODAL --- */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={filterPanelVisible}
        onRequestClose={() => setFilterPanelVisible(false)}
      >
        <View style={styles.drawerOverlay}>
          <Pressable style={styles.drawerDismissZone} onPress={() => setFilterPanelVisible(false)} />
          
          <View style={[styles.drawerSheetContainer, { backgroundColor: theme.card }]}>
            <SafeAreaView edges={['bottom']}>
              <View style={[styles.drawerHeader, { borderBottomColor: theme.border }]}>
                <Text style={[styles.drawerTitle, { color: theme.title }]}>Quiz Settings</Text>
                <Pressable onPress={() => setFilterPanelVisible(false)} style={styles.drawerCloseBtn}>
                  <FontAwesome5 name="times" size={16} color={theme.title} />
                </Pressable>
              </View>

              <ScrollView style={styles.drawerBodyContent} showsVerticalScrollIndicator={false}>
                {/* 1. DATA SOURCE SELECTOR */}
                <Text style={[styles.groupHeadingLabel, { color: theme.subtext }]}>Data Source</Text>
                <View style={styles.chipClusterFlexRow}>
                  <Pressable
                    onPress={() => setSourceMode('ai')}
                    style={[styles.filterChip, sourceMode === 'ai' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, sourceMode === 'ai' ? { color: '#FFF' } : { color: theme.title }]}>
                      ⚡ AI Generated
                    </Text>
                  </Pressable>

                  <Pressable
                    onPress={() => setSourceMode('pastpaper')}
                    style={[styles.filterChip, sourceMode === 'pastpaper' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, sourceMode === 'pastpaper' ? { color: '#FFF' } : { color: theme.title }]}>
                      📄 Saved Past Papers
                    </Text>
                  </Pressable>
                </View>

                {/* 2. QUESTION FORMAT */}
                <Text style={[styles.groupHeadingLabel, { color: theme.subtext, marginTop: 22 }]}>Question Format</Text>
                <View style={styles.chipClusterFlexRow}>
                  <Pressable
                    onPress={() => setSelectedType('mcq')}
                    style={[styles.filterChip, selectedType === 'mcq' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, selectedType === 'mcq' ? { color: '#FFF' } : { color: theme.title }]}>Multiple Choice</Text>
                  </Pressable>

                  <Pressable
                    onPress={() => setSelectedType('tf')}
                    style={[styles.filterChip, selectedType === 'tf' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, selectedType === 'tf' ? { color: '#FFF' } : { color: theme.title }]}>True / False</Text>
                  </Pressable>

                  <Pressable
                    onPress={() => setSelectedType('sa')}
                    style={[styles.filterChip, selectedType === 'sa' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, selectedType === 'sa' ? { color: '#FFF' } : { color: theme.title }]}>Short Answer</Text>
                  </Pressable>

                  <Pressable
                    onPress={() => setSelectedType('seq')}
                    style={[styles.filterChip, selectedType === 'seq' ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, selectedType === 'seq' ? { color: '#FFF' } : { color: theme.title }]}>Essay (SEQ)</Text>
                  </Pressable>
                </View>

                {/* 3. SUBJECT FILTER */}
                <Text style={[styles.groupHeadingLabel, { color: theme.subtext, marginTop: 22 }]}>Subject</Text>
                <View style={styles.chipClusterFlexRow}>
                  <Pressable
                    onPress={() => setSelectedSubject(null)}
                    style={[styles.filterChip, !selectedSubject ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, !selectedSubject ? { color: '#FFF' } : { color: theme.title }]}>All Subjects</Text>
                  </Pressable>
                  {subjects.map((sub) => {
                    const active = selectedSubject === sub;
                    return (
                      <Pressable
                        key={sub}
                        onPress={() => setSelectedSubject(sub)}
                        style={[styles.filterChip, active ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.filterChipText, active ? { color: '#FFF' } : { color: theme.title }]}>{sub}</Text>
                      </Pressable>
                    );
                  })}
                </View>

                {/* 4. TERM FILTER */}
                <Text style={[styles.groupHeadingLabel, { color: theme.subtext, marginTop: 22 }]}>Term / Batch</Text>
                <View style={styles.chipClusterFlexRow}>
                  <Pressable
                    onPress={() => setSelectedTerm(null)}
                    style={[styles.filterChip, !selectedTerm ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.filterChipText, !selectedTerm ? { color: '#FFF' } : { color: theme.title }]}>All Terms</Text>
                  </Pressable>
                  {terms.map((trm) => {
                    const active = selectedTerm === trm;
                    return (
                      <Pressable
                        key={trm}
                        onPress={() => setSelectedTerm(trm)}
                        style={[styles.filterChip, active ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.filterChipText, active ? { color: '#FFF' } : { color: theme.title }]}>Term {trm}</Text>
                      </Pressable>
                    );
                  })}
                </View>

                <View style={styles.drawerFooterGroup}>
                  <Pressable onPress={resetFilters} style={[styles.footerBtnSecondary, { borderColor: theme.border }]}>
                    <Text style={{ color: theme.title, fontWeight: '600' }}>Reset Options</Text>
                  </Pressable>
                  <Pressable onPress={() => setFilterPanelVisible(false)} style={[styles.footerBtnPrimary, { backgroundColor: theme.accent }]}>
                    <Text style={{ color: '#FFF', fontWeight: '700' }}>Recompile Pool</Text>
                  </Pressable>
                </View>
              </ScrollView>
            </SafeAreaView>
          </View>
        </View>
      </Modal>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  controlRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, marginTop: 16, marginBottom: 5, gap: 12 },
  summaryTextContainer: { flex: 1 },
  summaryLabel: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 2 },
  filterActionButton: { width: 44, height: 44, borderRadius: 12, justifyContent: 'center', alignItems: 'center', borderWidth: 1 },
  scroll: { flex: 1, paddingHorizontal: 20 },
  emptyContainer: { alignItems: 'center', marginTop: 80, width: '100%', paddingHorizontal: 20 },
  emptyText: { marginTop: 15, fontSize: 15, fontWeight: '500', textAlign: 'center' },
  inlineClearBtn: { marginTop: 16, paddingHorizontal: 16, paddingVertical: 8, borderRadius: 10, borderWidth: 1 },
  scoreContainer: { paddingHorizontal: 24, paddingVertical: 12, borderRadius: 16, borderWidth: 1, marginVertical: 15 },
  scoreText: { fontSize: 16, fontWeight: '700' },
  refreshButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', height: 48, borderRadius: 14, borderWidth: 1, marginTop: 10, marginBottom: 30, width: '100%' },
  refreshButtonText: { fontSize: 15, fontWeight: '700' },

  drawerOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  drawerDismissZone: { flex: 1 },
  drawerSheetContainer: { borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingBottom: 10, maxHeight: '85%' },
  drawerHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 18, borderBottomWidth: 1 },
  drawerTitle: { fontSize: 16, fontWeight: '700' },
  drawerCloseBtn: { padding: 4 },
  drawerBodyContent: { padding: 24 },
  groupHeadingLabel: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  chipClusterFlexRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  filterChip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 18, borderWidth: 1 },
  filterChipText: { fontSize: 13, fontWeight: '600' },
  drawerFooterGroup: { flexDirection: 'row', gap: 12, marginTop: 36, marginBottom: 24 },
  footerBtnSecondary: { flex: 1, height: 46, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  footerBtnPrimary: { flex: 2, height: 46, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
});