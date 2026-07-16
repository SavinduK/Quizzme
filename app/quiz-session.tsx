import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import DeleteModal from './components/delete-model';
import Footer from './components/footer';
import Header from './components/header';
import ModuleSelector from './components/module-selector';
import QuizCard from './components/quiz-card';
import QuizResults from './components/quiz-results';
import { Colors } from './constants/theme';

interface TargetFile {
  filename: string;
  subject: string;
  term: string;
  lesson: string;
}

interface QuizQuestion {
  question: string;
  options?: string[];
  correct_answer?: string; // Used for MCQ or pure Short Answer target text
  statements?: string[];
  answers?: boolean[];
  explanation?: string;   // Added to store context for grading/showing short answers
}

const FALLBACK_GEMINI_API_KEY = ""; 

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function QuestionSession() {
  const router = useRouter();
  const params = useLocalSearchParams<{ launchFilename?: string }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [filesMeta, setFilesMeta] = useState<TargetFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeDeck, setActiveDeck] = useState<QuizQuestion[] | null>(null);
  
  // Quiz Styles
  const [isTFQuiz, setIsTFQuiz] = useState<boolean>(false);
  const [isSAQuiz, setIsSAQuiz] = useState<boolean>(false);

  // Single Question Display & Quiz State Indexes
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState<number>(0);
  const [runningScore, setRunningScore] = useState<number>(0);
  const [quizFinished, setQuizFinished] = useState<boolean>(false);
  
  // Selection / Entry trackers
  const [chosenAnswer, setChosenAnswer] = useState<string | null>(null);
  const [tfSelections, setTfSelections] = useState<{ [key: number]: boolean | null }>({0: null, 1: null, 2: null, 3: null, 4: null});
  const [tfChecked, setTfChecked] = useState<boolean>(false);
  const [tfQuestionScore, setTfQuestionScore] = useState<number>(0); 
  
  // Short Answer Tracking
  const [saInputText, setSaInputText] = useState<string>("");
  const [saChecked, setSaChecked] = useState<boolean>(false);

  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [targetFilename, setTargetFilename] = useState<string | null>(null);

  const indexLocalFiles = async () => {
    try {
      const check = await FileSystem.getInfoAsync(QUESTIONS_DIR);
      if (!check.exists) return;
      const items = await FileSystem.readDirectoryAsync(QUESTIONS_DIR);
      const builds: TargetFile[] = [];
      for (const item of items) {
        if (item.endsWith('.txt')) {
          const cleanName = item.replace('.txt', '');
          const parts = cleanName.split('-');
          if (parts.length >= 3) {
            builds.push({
              filename: item,
              lesson: parts[0].replace(/_/g, ' '),
              subject: parts[1].replace(/_/g, ' '),
              term: parts[2].replace(/_/g, ' ')
            });
          }
        }
      }
      setFilesMeta(builds);
    } catch (e) { console.error(e); }
  };

  useFocusEffect(useCallback(() => { indexLocalFiles(); }, []));

  useEffect(() => {
    if (params.launchFilename) {
      launchDeck(params.launchFilename);
    }
  }, [params.launchFilename]);

  const handleDeleteFile = async () => {
    if (!targetFilename) return;
    try {
      const fileUri = `${QUESTIONS_DIR}${targetFilename}`;
      await FileSystem.deleteAsync(fileUri, { idempotent: true });
      const jsonFilename = targetFilename.replace('.txt', '.json');
      const cacheUri = `${CACHE_DIR}${jsonFilename}`;
      const cacheCheck = await FileSystem.getInfoAsync(cacheUri);
      if (cacheCheck.exists) await FileSystem.deleteAsync(cacheUri, { idempotent: true });
      setDeleteModalVisible(false);
      setTargetFilename(null);
      indexLocalFiles();
    } catch (e) { console.error(e); }
  };

  const openLessonReader = (file: TargetFile) => {
    router.push({
      pathname: '/read-view',
      params: { filename: file.filename, lesson: file.lesson }
    });
  };

  const launchDeck = async (filename: string) => {
    setLoading(true);
    setCurrentQuestionIdx(0);
    setRunningScore(0);
    setQuizFinished(false);
    setChosenAnswer(null);
    setTfSelections({0: null, 1: null, 2: null, 3: null, 4: null});
    setTfChecked(false);
    setTfQuestionScore(0);
    setSaInputText("");
    setSaChecked(false);

    const jsonCacheFilename = filename.replace('.txt', '.json');
    const specificCacheUri = `${CACHE_DIR}${jsonCacheFilename}`;

    try {
      let activeApiKey = FALLBACK_GEMINI_API_KEY;
      let targetCount = 5;
      let targetStyle = 'MCQ'; // Can be: 'MCQ', 'TF', or 'SA'
      let customPrompt = "";

      try {
        const keyFileCheck = await FileSystem.getInfoAsync(KEY_FILE_URI);
        if (keyFileCheck.exists) {
          const lines = (await FileSystem.readAsStringAsync(KEY_FILE_URI)).split('\n');
          if (lines[0]) activeApiKey = lines[0].trim();
          if (lines[1]) targetCount = parseInt(lines[1].trim(), 10) || 5;
          if (lines[2]) {
            const parsedStyle = lines[2].trim().toUpperCase();
            if (parsedStyle === 'TF') targetStyle = 'TF';
            else if (parsedStyle === 'SA') targetStyle = 'SA';
            else targetStyle = 'MCQ';
          }
          if (lines[3]) customPrompt = lines[3].trim();
        }
      } catch (keyError) {
        console.warn("Relying on default configurations.", keyError);
      }

      setIsTFQuiz(targetStyle === 'TF');
      setIsSAQuiz(targetStyle === 'SA');
      
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
      let prompt = "";
      
      if (targetStyle === 'MCQ') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} multiple choice questions. Each question must have exactly 5 distinct options. Return the data strictly as a JSON object containing an array called "questions". Each item in the array must contain "question" (string), "options" (array of 5 strings), and "correct_answer" (string matching exactly one of the options).${customPrompt} \nSource material text:${targetStr}`;
      } else if (targetStyle === 'TF') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} True/False style questions. Each item must contain a header topic text called "question", and an array of exactly 5 distinct conceptual statements related to it. For each statement, provide its corresponding boolean true/false answer value. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string context", "statements": ["s1", "s2", "s3", "s4", "s5"], "answers": [true, false, true, true, false]}]}.${customPrompt} \nSource material text:${targetStr}`;
      } else {
        // Short Answer Generation Strategy
        prompt = `Based on the following source material text, generate exactly ${targetCount} clear conceptual short answer questions. Return the data strictly as a JSON object containing an array called "questions". Each item must contain "question" (string), "correct_answer" (string representing the definitive brief answer key phrase), and "explanation" (string explaining the underlying core context completely).${customPrompt} \nSource material text:${targetStr}`;
      }

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { responseMimeType: "application/json" }
        })
      });

      const resData = await response.json();
      const rawJsonText = resData.candidates[0].content.parts[0].text;
      
      const cacheDirCheck = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!cacheDirCheck.exists) {
        await FileSystem.makeDirectoryAsync(CACHE_DIR, { intermediates: true });
      }
      await FileSystem.writeAsStringAsync(specificCacheUri, rawJsonText);

      const parsedQuiz = JSON.parse(rawJsonText);
      let targetDeck: QuizQuestion[] = parsedQuiz.questions || parsedQuiz;
      
      setActiveDeck(targetDeck);

    } catch (e) { 
      console.warn("Online generation failed or device offline. Checking workspace cache alternatives...", e);
      
      try {
        const localCacheCheck = await FileSystem.getInfoAsync(specificCacheUri);
        if (localCacheCheck.exists) {
          const rawCachedText = await FileSystem.readAsStringAsync(specificCacheUri);
          const parsedCache = JSON.parse(rawCachedText);
          let targetDeck: QuizQuestion[] = parsedCache.questions || parsedCache;
          
          setActiveDeck(targetDeck);
          Alert.alert("Offline Mode Active", "Loaded previously compiled questions from cache filesystem successfully.");
        } else {
          Alert.alert("Network Unavailable", "Could not query online servers, and no cached alternative exists for this module.");
        }
      } catch (cacheReadError) {
        console.error("Critical extraction failure handling cache data strings", cacheReadError);
        Alert.alert("Generation Failed", "Could not verify storage fallbacks.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleNextQuestion = () => {
    if (!activeDeck) return;
    if (currentQuestionIdx + 1 < activeDeck.length) {
      setCurrentQuestionIdx(p => p + 1);
      setChosenAnswer(null);
      setTfSelections({0: null, 1: null, 2: null, 3: null, 4: null});
      setTfChecked(false);
      setTfQuestionScore(0);
      setSaInputText("");
      setSaChecked(false);
    } else {
      setQuizFinished(true);
    }
  };

  const evaluateTfQuestion = () => {
    if (!activeDeck) return;
    const currentQ = activeDeck[currentQuestionIdx];
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

  const maxPossibleScore = activeDeck ? (isTFQuiz ? activeDeck.length * 5 : activeDeck.length) : 0;

  const copyToClipboard = async (filename: string) => {
    const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
    await Clipboard.setStringAsync(targetStr);
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <Header title="Lecture Questions" onRightButtonPress={() => router.push('/add-questions')} />

      {/* 1. Global Loading State */}
      {loading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={theme.accent} />
          <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>Compiling Questions...</Text>
        </View>
      )}

      {/* 2. Selection View */}
      {!loading && !activeDeck && (
        <ModuleSelector
          availableLessons={filesMeta}
          launchDeck={launchDeck}
          copyToClipboard={copyToClipboard}
          onSelectDeleteTarget={(filename) => { setTargetFilename(filename); setDeleteModalVisible(true); }}
          onSelectLesson={openLessonReader}
        />
      )}

      {/* 3. Quiz Game View */}
      {!loading && activeDeck && (
        <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
          <View>
            {quizFinished ? (
              <QuizResults  
                runningScore={runningScore} 
                maxPossibleScore={maxPossibleScore} 
                onReturn={() => setActiveDeck(null)} 
              />
            ) : (
              <QuizCard
                item={activeDeck[currentQuestionIdx]}
                chosenAnswer={chosenAnswer}
                runningScore={runningScore}
                maxPossibleScore={maxPossibleScore}
                setChosenAnswer={setChosenAnswer}
                setRunningScore={setRunningScore}
                tfSelections={tfSelections}
                setTfSelections={setTfSelections}
                tfChecked={tfChecked}
                tfQuestionScore={tfQuestionScore}
                evaluateTfQuestion={evaluateTfQuestion}
                handleNextQuestion={handleNextQuestion}
                currentQuestionIdx={currentQuestionIdx}
                totalQuestions={activeDeck.length}
                // Short Answer bindings to pass down to your component UI logic
                isSAQuiz={isSAQuiz}
                saInputText={saInputText}
                setSaInputText={setSaInputText}
                saChecked={saChecked}
                setSaChecked={setSaChecked}
              />
            )}
          </View>
        </ScrollView>
      )}

      <DeleteModal visible={deleteModalVisible} onCancel={() => setDeleteModalVisible(false)} onConfirm={handleDeleteFile} />
      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
});