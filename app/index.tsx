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

const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

type QuestionType = 'mcq' | 'tf';

export default function HomeFeed() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [subjects, setSubjects] = useState<string[]>([]);
  const [terms, setTerms] = useState<string[]>([]);
  
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  const [selectedType, setSelectedType] = useState<QuestionType>('mcq');

  const [compiledPool, setCompiledPool] = useState<any[]>([]);
  const [quizFinished, setQuizFinished] = useState<boolean>(false);
  
  // Dynamic pool limit state parsed from second line of key.txt (Defaults to 5)
  const [maxQuestionsCount, setMaxQuestionsCount] = useState<number>(5);
  
  // Single Card Operational State Managers
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [runningScore, setRunningScore] = useState<number>(0);
  const [chosenAnswer, setChosenAnswer] = useState<string | null>(null);
  
  // True/False granular breakdown evaluation tracking states
  const [tfSelections, setTfSelections] = useState<{ [key: number]: boolean | null }>({ 0: null, 1: null, 2: null, 3: null, 4: null });
  const [tfChecked, setTfChecked] = useState<boolean>(false);
  const [tfQuestionScore, setTfQuestionScore] = useState<number>(0);

  const [subPickerVisible, setSubPickerVisible] = useState(false);
  const [termPickerVisible, setTermPickerVisible] = useState(false);
  const [typePickerVisible, setTypePickerVisible] = useState(false);

  const fetchAvailableDecks = async () => {
    try {
      // --- Read target question count bounds configurations from key.txt ---
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

      const folderInfo = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!folderInfo.exists) return;

      const files = await FileSystem.readDirectoryAsync(CACHE_DIR);
      const uniqueSubjects = new Set<string>();
      const uniqueTerms = new Set<string>();

      for (const file of files) {
        if (file.endsWith('.json')) {
          const cleanName = file.replace('.json', '');
          const parts = cleanName.split('-');
          
          if (parts.length >= 3) {
            const parsedSubject = parts[1].replace(/_/g, ' ');
            const parsedTerm = parts[2].replace(/_/g, ' ');
            uniqueSubjects.add(parsedSubject);
            uniqueTerms.add(parsedTerm);
          }
        }
      }
      setSubjects(Array.from(uniqueSubjects));
      setTerms(Array.from(uniqueTerms));
    } catch (e) { console.error("Error reading data file systems:", e); }
  };

  const generateRandomSession = async () => {
  try {
    const folderInfo = await FileSystem.getInfoAsync(CACHE_DIR);
    if (!folderInfo.exists) return;

    const files = await FileSystem.readDirectoryAsync(CACHE_DIR);
    console.log(files);
    let rawQuestions: any[] = [];

    for (const file of files) {
      if (!file.endsWith('.json')) continue;
      
      const cleanName = file.replace('.json', '');
      const parts = cleanName.split('-');
      
      if (parts.length >= 3) {
        const fileSubject = parts[1].replace(/_/g, ' ');
        const fileTerm = parts[2].replace(/_/g, ' ');

        const matchSubject = !selectedSubject || fileSubject === selectedSubject;
        const matchTerm = !selectedTerm || fileTerm === selectedTerm;

        if (matchSubject && matchTerm) {
          const rawContent = await FileSystem.readAsStringAsync(`${CACHE_DIR}${file}`);
          if (!rawContent || rawContent.trim() === '') continue;

          try {
            const parsed = JSON.parse(rawContent);
            console.log(parsed);
            let questionsList: any[] = [];
            
            if (Array.isArray(parsed)) {
              questionsList = parsed;
            } else if (parsed && Array.isArray(parsed.questions)) {
              questionsList = parsed.questions;
            }

            // MODIFIED FILTER LOGIC: Sort by structural keys
            questionsList = questionsList.filter((q) => {
              let qType = '';

              // Identify TF: Contains 'statements' and 'answers' arrays
              if (Array.isArray(q.statements) && Array.isArray(q.answers)) {
                qType = 'tf';
              } 
              // Identify MCQ: Contains 'options' array and 'correct_answer' string
              else if (Array.isArray(q.options) && typeof q.correct_answer === 'string') {
                qType = 'mcq';
              } else if (q.type) {
                // Fallback just in case some questions explicitly declare their type
                qType = q.type.toLowerCase();
              }

              return qType === selectedType.toLowerCase();
            });

            rawQuestions.push(...questionsList);
          } catch (err) {
            console.error(`Error parsing JSON in ${file}:`, err);
          }}
      }}

    // Dynamic slicing calculation mapped to configured file states
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
  };

  useFocusEffect(useCallback(() => { fetchAvailableDecks(); }, []));
  useFocusEffect(useCallback(() => { generateRandomSession(); }, [selectedSubject, selectedTerm, selectedType, maxQuestionsCount]));

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
    } else {
      setQuizFinished(true);
    }
  };

  const getTypeLabel = (type: QuestionType) => {
    return type === 'mcq' ? 'MCQs Only' : 'T/F Only';
  };

  const maxPossibleScore = selectedType === 'tf' ? compiledPool.length * 5 : compiledPool.length;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      <Header title="Daily Flash-Quizzes" onRightButtonPress={() => router.push('/add-questions')} />

      {/* FILTERS DROPDOWN COMPONENT BASE */}
      <View style={styles.dropdownRow}>
        <Pressable style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={() => setSubPickerVisible(true)}>
          <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>{selectedSubject ?? "All Subjects"}</Text>
          <FontAwesome5 name="chevron-down" size={10} color={theme.subtext} />
        </Pressable>

        <Pressable style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={() => setTermPickerVisible(true)}>
          <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>{selectedTerm ?? "All Terms"}</Text>
          <FontAwesome5 name="chevron-down" size={10} color={theme.subtext} />
        </Pressable>

        <Pressable style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={() => setTypePickerVisible(true)}>
          <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>{getTypeLabel(selectedType)}</Text>
          <FontAwesome5 name="chevron-down" size={10} color={theme.subtext} />
        </Pressable>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {compiledPool.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="graduation-cap" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No datasets matched selection.</Text>
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
          /* SINGLE CARD INTERACTION INJECTOR MODULE */
          <QuizCard
            item={compiledPool[currentQuestionIndex]}
            chosenAnswer={chosenAnswer}
            runningScore={runningScore}
            maxPossibleScore={maxPossibleScore}
            setChosenAnswer={(answer) => {
              // Isolated point evaluation safely inside the individual card component
              setChosenAnswer(answer);
            }}
            setRunningScore={setRunningScore}
            tfSelections={tfSelections}
            setTfSelections={setTfSelections}
            tfChecked={tfChecked}
            tfQuestionScore={tfQuestionScore}
            evaluateTfQuestion={evaluateTfQuestion}
            handleNextQuestion={handleNextQuestion}
            currentQuestionIdx={currentQuestionIndex}
            totalQuestions={compiledPool.length}
          />
        )}
      </ScrollView>

      {/* MODAL CONFIGURATIONS */}
      <Modal visible={subPickerVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setSubPickerVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Subject</Text>
            <ScrollView style={{ width: '100%', maxHeight: 300 }}>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedSubject(null); setSubPickerVisible(false); }}>
                <Text style={{ color: theme.accent, fontWeight: '700' }}>All Subjects</Text>
              </Pressable>
              {subjects.map(sub => (
                <Pressable key={sub} style={styles.modalItem} onPress={() => { setSelectedSubject(sub); setSubPickerVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '500' }}>{sub}</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>

      <Modal visible={termPickerVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setTermPickerVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Term</Text>
            <ScrollView style={{ width: '100%', maxHeight: 300 }}>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedTerm(null); setTermPickerVisible(false); }}>
                <Text style={{ color: theme.accent, fontWeight: '700' }}>All Terms</Text>
              </Pressable>
              {terms.map(t => (
                <Pressable key={t} style={styles.modalItem} onPress={() => { setSelectedTerm(t); setTermPickerVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '500' }}>{t}</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>

      <Modal visible={typePickerVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setTypePickerVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Question Type</Text>
            <ScrollView style={{ width: '100%', maxHeight: 300 }}>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedType('mcq'); setTypePickerVisible(false); }}>
                <Text style={{ color: theme.title, fontWeight: '500' }}>Multiple Choice (MCQs)</Text>
              </Pressable>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedType('tf'); setTypePickerVisible(false); }}>
                <Text style={{ color: theme.title, fontWeight: '500' }}>True / False</Text>
              </Pressable>
            </ScrollView>
          </View>
        </Pressable>
      </Modal>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  dropdownRow: { flexDirection: 'row', paddingHorizontal: 25, marginVertical: 15, gap: 8 },
  dropdown: { flex: 1, height: 46, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 10 },
  dropdownText: { fontSize: 12, fontWeight: '600', flex: 1, marginRight: 4 },
  scroll: { flex: 1, paddingHorizontal: 20 },
  emptyContainer: { alignItems: 'center', marginTop: 80, width: '100%' },
  emptyText: { marginTop: 15, fontSize: 15, fontWeight: '500' },
  scoreContainer: { paddingHorizontal: 24, paddingVertical: 12, borderRadius: 16, borderWidth: 1, marginVertical: 15 },
  scoreText: { fontSize: 16, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', borderRadius: 24, padding: 20, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 15 },
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' },
  refreshButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', height: 48, borderRadius: 14, borderWidth: 1, marginTop: 10, marginBottom: 30, width: '100%' },
  refreshButtonText: { fontSize: 15, fontWeight: '700' },
});