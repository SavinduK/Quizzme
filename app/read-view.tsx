import { FontAwesome5 } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useLocalSearchParams, useRouter } from 'expo-router';
import * as Sharing from 'expo-sharing';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from 'react-native';
import Markdown from 'react-native-markdown-display';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import QuizCard from './components/quiz-card';
import QuizResults from './components/quiz-results';
import { Colors } from './constants/theme';

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;
const SUMMARY_DIR = `${FileSystem.documentDirectory}summaries/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;
const FALLBACK_GEMINI_API_KEY = "";

interface SeqSubQuestion {
  sub_question: string;
  marks?: number;
  answer_key: string;
}

interface QuizQuestion {
  question: string;
  options?: string[];
  correct_answer?: string;
  statements?: string[];
  answers?: boolean[];
  explanation?: string;
  sub_questions?: SeqSubQuestion[];
  model_answer?: string;
}
interface CardBlock {
  id: string;
  title: string;
  content: string;
  type: 'heading' | 'paragraph' | 'quote';
}

function parseMarkdownToCards(markdown: string): CardBlock[] {
  if (!markdown) return [];
  const sections = markdown.split(/\n(?=# |\n)/g).filter(Boolean);

  return sections.reduce((acc, sec) => {
    const lines = sec.trim().split('\n');
    let content = sec.trim();

    if (lines[0].startsWith('#')) {
      const title = lines[0].replace(/^#+\s*/, '');
      content = lines.slice(1).join('\n').trim() || title;
      
      acc.push({
        id: `block-${acc.length}`, // Use acc.length to keep sequential IDs
        title,
        content,
        type: 'heading',
      });
    } 
    else if (lines[0].startsWith('>')) {
      acc.push({
        id: `block-${acc.length}`,
        title: 'Key Note',
        content,
        type: 'quote',
      });
    } 
    else {
      if (acc.length > 0) {
        const lastCard = acc[acc.length - 1];
        lastCard.content += `\n\n${content}`;
      } else {
        acc.push({
          id: `block-${acc.length}`,
          title: 'Overview', 
          content,
          type: 'paragraph',
        });
      }
    }

    return acc;
  }, [] as CardBlock[]);
}

export default function LessonReaderScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ filename: string; lesson: string; initialTab?: 'notes' | 'quiz' | 'summary' }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [loading, setLoading] = useState(true);
  const [readingContent, setReadingContent] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'notes' | 'quiz' | 'summary'>(params.initialTab || 'notes');

  // Summary State
  const [summaryContent, setSummaryContent] = useState<string>('');
  const [loadingSummary, setLoadingSummary] = useState<boolean>(false);
  const [summaryStatus, setSummaryStatus] = useState<string>('Checking context...');

  // Quiz Engine State
  const [loadingQuiz, setLoadingQuiz] = useState(false);
  const [activeDeck, setActiveDeck] = useState<QuizQuestion[] | null>(null);
  const [isTFQuiz, setIsTFQuiz] = useState<boolean>(false);
  const [isSAQuiz, setIsSAQuiz] = useState<boolean>(false);
  const [isSEQQuiz, setIsSEQQuiz] = useState<boolean>(false);
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState<number>(0);
  const [runningScore, setRunningScore] = useState<number>(0);
  const [quizFinished, setQuizFinished] = useState<boolean>(false);

  // Quiz Selection / Entry Trackers
  const [chosenAnswer, setChosenAnswer] = useState<string | null>(null);
  const [tfSelections, setTfSelections] = useState<{ [key: number]: boolean | null }>({ 0: null, 1: null, 2: null, 3: null, 4: null });
  const [tfChecked, setTfChecked] = useState<boolean>(false);
  const [tfQuestionScore, setTfQuestionScore] = useState<number>(0);
  const [saInputText, setSaInputText] = useState<string>("");
  const [saChecked, setSaChecked] = useState<boolean>(false);
  const [showSeqAnswer, setShowSeqAnswer] = useState<boolean>(false);
  const [seqUserNotes, setSeqUserNotes] = useState<string>("");

  // Editing state
  const [isEditing, setIsEditing] = useState(false);
  const [editableText, setEditableText] = useState('');
  const [saving, setSaving] = useState(false);
  const [selection, setSelection] = useState<{ start: number; end: number } | undefined>(undefined);
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  useEffect(() => {
    const loadContent = async () => {
      if (!params.filename) {
        Alert.alert('Error', 'No lesson file specified.');
        router.back();
        return;
      }
      try {
        const content = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
        setReadingContent(content);
      } catch (e) {
        console.error(e);
        Alert.alert('Error', 'Could not load lesson contents.');
        router.back();
      } finally {
        setLoading(false);
      }
    };

    loadContent();
  }, [params.filename]);

  useEffect(() => {
  if (params.initialTab === 'quiz' && !activeDeck && !loadingQuiz) {
    launchDeck();
  }
  }, [params.initialTab]);

  const copyToClipboard = async () => {
    if (!params.filename) return;
    try {
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
      await Clipboard.setStringAsync(targetStr);
    } catch (e) {
      console.error(e);
    }
  };

  const shareAsMarkdown = async () => {
      const isAvailable = await Sharing.isAvailableAsync();
      if (!isAvailable) {
        alert("Sharing isn't available on this device");
        return;
      }
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
      const baseName = params.filename.substring(0, params.filename.lastIndexOf('.')) || params.filename;
      const tempMdPath = `${FileSystem.cacheDirectory}${baseName}.md`;
  
      await FileSystem.writeAsStringAsync(tempMdPath, targetStr, {
        encoding: FileSystem.EncodingType.UTF8,
      });
      await Sharing.shareAsync(tempMdPath, {
        mimeType: 'text/markdown',
        dialogTitle: `Share ${baseName}.md`,
        UTI: 'net.daringfireball.markdown', // Helps iOS recognize the file type
      });
    };

  const resetQuestionStates = () => {
    setChosenAnswer(null);
    setTfSelections({ 0: null, 1: null, 2: null, 3: null, 4: null });
    setTfChecked(false);
    setTfQuestionScore(0);
    setSaInputText("");
    setSaChecked(false);
    setShowSeqAnswer(false);
    setSeqUserNotes("");
  };

  const fetchApiKeyConfig = async () => {
    let activeApiKey = FALLBACK_GEMINI_API_KEY;
    let targetCount = 5;
    let targetStyle = 'MCQ';
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
          else if (parsedStyle === 'SEQ') targetStyle = 'SEQ';
          else targetStyle = 'MCQ';
        }
        if (lines[3]) customPrompt = lines[3].trim();
      }
    } catch (e) {
      console.warn("Using default key settings.", e);
    }

    return { activeApiKey, targetCount, targetStyle, customPrompt };
  };

  const launchDeck = async () => {
    if (!params.filename) return;
    setLoadingQuiz(true);
    setCurrentQuestionIdx(0);
    setRunningScore(0);
    setQuizFinished(false);
    resetQuestionStates();

    const jsonCacheFilename = params.filename.replace('.txt', '.json');
    const specificCacheUri = `${CACHE_DIR}${jsonCacheFilename}`;

    try {
      const { activeApiKey, targetCount, targetStyle, customPrompt } = await fetchApiKeyConfig();

      setIsTFQuiz(targetStyle === 'TF');
      setIsSAQuiz(targetStyle === 'SA');
      setIsSEQQuiz(targetStyle === 'SEQ');

      const targetStr = readingContent || await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
      let prompt = "";

      if (targetStyle === 'MCQ') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} multiple choice questions. Each question must have exactly 5 distinct options. Provide a brief explanation for why the correct answer is correct. Return the data strictly as a JSON object containing an array called "questions". Each item in the array must contain "question" (string), "options" (array of 5 strings), "correct_answer" (string matching exactly one of the options), and "explanation" (string).${customPrompt} \nSource material text:${targetStr}`;
      } else if (targetStyle === 'TF') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} True/False style questions. Each item must contain a header topic text called "question", and an array of exactly 5 distinct conceptual statements related to it. For each statement, provide its corresponding boolean true/false answer value. Include an "explanation" string giving a concise rationale for the overall set or key statements. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string context", "statements": ["s1", "s2", "s3", "s4", "s5"], "answers": [true, false, true, true, false], "explanation": "string rationale"}]}.${customPrompt} \nSource material text:${targetStr}`;
      } else if (targetStyle === 'SA') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} clear conceptual short answer questions. Return the data strictly as a JSON object containing an array called "questions". Each item must contain "question" (string), "correct_answer" (string representing the definitive brief answer key phrase), and "explanation" (string explaining the underlying core context completely).${customPrompt} \nSource material text:${targetStr}`;
      } else {
        prompt = `Based on the complete source material provided, construct ${targetCount} comprehensive Structured Essay Questions (SEQs) that systematically cover the entire lecture content. Each SEQ must revolve around a core topic from the lecture and contain 2 to 4 sub-questions ranging from short recall/definitions to detailed analytical essay prompts. Provide exhaustive model answers and structured marking rubrics for every sub-question. Return strictly a JSON object with a key "questions" containing an array. Structure: {"questions": [{"question": "Main Topic", "sub_questions": [{"sub_question": "(a) Define X...", "marks": 5, "answer_key": "Details"}], "model_answer": "Complete essay"}]}.${customPrompt} \nSource material text:${targetStr}`;
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
      const targetDeck: QuizQuestion[] = parsedQuiz.questions || parsedQuiz;
      setActiveDeck(targetDeck);

    } catch (e) {
      console.warn("Generation failed. Checking cache...", e);
      try {
        const localCacheCheck = await FileSystem.getInfoAsync(specificCacheUri);
        if (localCacheCheck.exists) {
          const rawCachedText = await FileSystem.readAsStringAsync(specificCacheUri);
          const parsedCache = JSON.parse(rawCachedText);
          const targetDeck: QuizQuestion[] = parsedCache.questions || parsedCache;
          setActiveDeck(targetDeck);
          Alert.alert("Offline Mode Active", "Loaded cached questions successfully.");
        } else {
          Alert.alert("Network Unavailable", "Could not connect to service and no cache exists.");
        }
      } catch (cacheErr) {
        Alert.alert("Error", "Could not load quiz questions.");
      }
    } finally {
      setLoadingQuiz(false);
    }
  };

  const fetchOrGenerateSummary = async () => {
    if (summaryContent) return;
    if (!params.filename) return;

    setLoadingSummary(true);
    setSummaryStatus("Checking context...");

    const summaryFilename = `summary-${params.filename}`;
    const summaryFileUri = `${SUMMARY_DIR}${summaryFilename}`;
    const sourceFileUri = `${QUESTIONS_DIR}${params.filename}`;

    try {
      // 1. Ensure output directory exists
      const summaryDirCheck = await FileSystem.getInfoAsync(SUMMARY_DIR);
      if (!summaryDirCheck.exists) {
        await FileSystem.makeDirectoryAsync(SUMMARY_DIR, { intermediates: true });
      }

      // 2. Check for cached summary locally
      const summaryCheck = await FileSystem.getInfoAsync(summaryFileUri);
      if (summaryCheck.exists) {
        const cachedSummary = await FileSystem.readAsStringAsync(summaryFileUri);
        setSummaryContent(cachedSummary);
        setLoadingSummary(false);
        return;
      }

      // 3. Read source text
      setSummaryStatus("Reading source content...");
      const sourceText = readingContent || await FileSystem.readAsStringAsync(sourceFileUri);

      // 4. Extract active configurations/Keys
      setSummaryStatus("Connecting to AI engine...");
      const { activeApiKey } = await fetchApiKeyConfig();

      // 5. Fire structured prompt
      const prompt = `You are a world-class academic summarizer. Read the following source text and provide a comprehensive, clear, and highly structured summary. Use Markdown features natively: clear Headings (# and ##), bulleted takeaway lists, bold terms for critical definitions, and short focused paragraphs. Avoid code block syntax envelopes or wrappers—just send the raw Markdown text.\n\nSource Text:\n${sourceText}`;

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      });

      const resData = await response.json();
      const rawSummaryMarkdown = resData.candidates[0].content.parts[0].text;

      // 6. Cache and set state
      await FileSystem.writeAsStringAsync(summaryFileUri, rawSummaryMarkdown);
      setSummaryContent(rawSummaryMarkdown);

    } catch (error) {
      console.error("Summary processing failure:", error);
      Alert.alert("Generation Failed", "Could not structure a summary. Please check your network connection.");
    } finally {
      setLoadingSummary(false);
    }
  };

  const handleTabPress = (tab: 'notes' | 'quiz' | 'summary') => {
    setActiveTab(tab);
    if (tab === 'quiz' && !activeDeck && !loadingQuiz) {
      launchDeck();
    } else if (tab === 'summary' && !summaryContent && !loadingSummary) {
      fetchOrGenerateSummary();
    }
  };

  const handleNextQuestion = () => {
    if (!activeDeck) return;
    if (currentQuestionIdx + 1 < activeDeck.length) {
      setCurrentQuestionIdx(p => p + 1);
      resetQuestionStates();
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
        if (selected === realAnswer) calculatedQScore += 1;
        else calculatedQScore -= 1;
      }
    }

    const finalClampedQScore = Math.max(0, calculatedQScore);
    setTfQuestionScore(finalClampedQScore);
    setRunningScore(p => p + finalClampedQScore);
    setTfChecked(true);
  };

  const maxPossibleScore = activeDeck
    ? (isTFQuiz ? activeDeck.length * 5 : (isSEQQuiz ? activeDeck.length * 10 : activeDeck.length))
    : 0;

  const handleOpenEditModal = () => {
    setEditableText(readingContent);
    setSelection({ start: 0, end: 0 });
    setIsEditing(true);
  };

  const handleSaveEdit = async () => {
    if (!params.filename) return;
    setSaving(true);
    try {
      const filePath = `${QUESTIONS_DIR}${params.filename}`;
      await FileSystem.writeAsStringAsync(filePath, editableText);
      setReadingContent(editableText);
      
      // Invalidate cached summary upon saving edits
      const summaryFilename = `summary-${params.filename}`;
      const summaryFileUri = `${SUMMARY_DIR}${summaryFilename}`;
      const summaryCheck = await FileSystem.getInfoAsync(summaryFileUri);
      if (summaryCheck.exists) {
        await FileSystem.deleteAsync(summaryFileUri, { idempotent: true });
      }
      setSummaryContent('');

      setIsEditing(false);
      setShowSuccessModal(true);
    } catch (e) {
      console.error(e);
      Alert.alert('Error', 'Failed to save edits.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: theme.background, justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={theme.accent} />
        <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>Loading Content...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      {/* Header */}
      <View style={[styles.readerHeader, { borderBottomColor: theme.border }]}>
        <View style={{ flexDirection: 'row', alignItems: 'center', width: '100%' }}>
          <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
            <FontAwesome5 name="arrow-left" size={16} color={theme.text} />
          </TouchableOpacity>
          <View style={{ flex: 1, marginRight: 10 }}>
            <Text
              style={[styles.lessonTitleLarge, { color: theme.text, marginHorizontal: 10 }]}
              numberOfLines={1}
              ellipsizeMode="tail"
            >
              {params.lesson || 'View Lesson'}
            </Text>
          </View>

          <TouchableOpacity style={[styles.iconShareBtn, ]} onPress={copyToClipboard}>
            <FontAwesome5 name="copy" size={16} color={theme.accent} />
          </TouchableOpacity>

          <TouchableOpacity style={[styles.iconShareBtn, ]} onPress={shareAsMarkdown}>
            <FontAwesome5 name="share-alt" size={16} color={theme.accent} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Horizontal Tab Navigation */}
      <View style={[styles.tabContainer, { backgroundColor: theme.buttons ?? '#f2f2f2' }]}>
        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'notes' && [styles.activeTab, { backgroundColor: theme.background }]]}
          onPress={() => handleTabPress('notes')}
        >
          <FontAwesome5 name="book-open" size={13} color={activeTab === 'notes' ? theme.accent : theme.subtext} />
          <Text style={[styles.tabText, { color: activeTab === 'notes' ? theme.accent : theme.subtext }]}>Notes</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'quiz' && [styles.activeTab, { backgroundColor: theme.background }]]}
          onPress={() => handleTabPress('quiz')}
        >
          <FontAwesome5 name="bolt" size={13} color={activeTab === 'quiz' ? theme.accent : theme.subtext} />
          <Text style={[styles.tabText, { color: activeTab === 'quiz' ? theme.accent : theme.subtext }]}>Quiz</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'summary' && [styles.activeTab, { backgroundColor: theme.background }]]}
          onPress={() => handleTabPress('summary')}
        >
          <FontAwesome5 name="file-alt" size={13} color={activeTab === 'summary' ? theme.accent : theme.subtext} />
          <Text style={[styles.tabText, { color: activeTab === 'summary' ? theme.accent : theme.subtext }]}>Summary</Text>
        </TouchableOpacity>
      </View>

      {/* Dynamic Tab Body */}
      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {activeTab === 'notes' && (
          <Markdown
            style={{
              body: { color: theme.text, fontSize: 15, lineHeight: 24 },
              heading1: { color: theme.text, fontWeight: '700', marginVertical: 10 },
              heading2: { color: theme.text, fontWeight: '600', marginVertical: 8 },
              paragraph: { marginVertical: 6 },
              link: { color: theme.accent },
              bullet_list: { color: theme.text },
              ordered_list: { color: theme.text },
            }}
          >
            {readingContent}
          </Markdown>
        )}

        {activeTab === 'quiz' && (
          <View>
            {loadingQuiz && (
              <View style={styles.loadingContainer}>
                <ActivityIndicator size="large" color={theme.accent} />
                <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>
                  Compiling Questions & Answer Keys...
                </Text>
              </View>
            )}

            {!loadingQuiz && activeDeck && (
              <View>
                {quizFinished ? (
                  <QuizResults
                    runningScore={runningScore}
                    maxPossibleScore={maxPossibleScore}
                    onReturn={() => launchDeck()}
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
                    isSAQuiz={isSAQuiz}
                    saInputText={saInputText}
                    setSaInputText={setSaInputText}
                    saChecked={saChecked}
                    setSaChecked={setSaChecked}
                    isSEQQuiz={isSEQQuiz}
                    showSeqAnswer={showSeqAnswer}
                    setShowSeqAnswer={setShowSeqAnswer}
                    seqUserNotes={seqUserNotes}
                    setSeqUserNotes={setSeqUserNotes}
                  />
                )}
              </View>
            )}
          </View>
        )}

              {/* Active Tab: Summary */}
      {activeTab === 'summary' && (
        <View style={styles.cardsContainer}>
          {loadingSummary ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color={theme.accent} />
              <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>
                {summaryStatus}
              </Text>
            </View>
          ) : (
            parseMarkdownToCards(summaryContent).map((block, idx) => (
              <View 
                key={block.id} 
                style={[
                  styles.card, 
                  { 
                    backgroundColor: theme.card ?? theme.buttons ?? '#1E1E1E',
                    borderColor: theme.border, 
                  }
                ]}
              >
                <View style={styles.cardHeader}>
                  <Text style={[styles.cardTitle, { color: theme.accent }]}>
                    {block.title}
                  </Text>
                </View>

                <Markdown
                  style={{
                    body: { color: theme.text, fontSize: 15, lineHeight: 24 },
                    strong: { fontWeight: '700', color: theme.text },
                    paragraph: { marginVertical: 4 },
                    link: { color: theme.accent },
                  }}
                >
                  {block.content}
                </Markdown>
              </View>
            ))
          )}
        </View>
      )}
      </ScrollView>

      {/* Floating Action Button (FAB) for Editing Notes */}
      {activeTab === 'notes' && (
        <TouchableOpacity
          style={[styles.fab, { backgroundColor: theme.accent }]}
          onPress={handleOpenEditModal}
          activeOpacity={0.8}
        >
          <FontAwesome5 name="pen" size={18} color="#FFFFFF" />
        </TouchableOpacity>
      )}

      {/* Edit Modal */}
      <Modal visible={isEditing} animationType="slide" presentationStyle="pageSheet" onRequestClose={() => setIsEditing(false)}>
        <SafeAreaView style={[styles.modalContainer, { backgroundColor: theme.background }]}>
          <View style={[styles.modalHeader, { borderBottomColor: theme.border }]}>
            <TouchableOpacity onPress={() => setIsEditing(false)}>
              <Text style={[styles.modalCancel, { color: theme.subtext }]}>Cancel</Text>
            </TouchableOpacity>
            <Text style={[styles.modalTitle, { color: theme.text }]}>Edit Lesson</Text>
            <TouchableOpacity onPress={handleSaveEdit} disabled={saving}>
              {saving ? (
                <ActivityIndicator size="small" color={theme.accent} />
              ) : (
                <Text style={[styles.modalSave, { color: theme.accent }]}>Save</Text>
              )}
            </TouchableOpacity>
          </View>

          <TextInput
            style={[styles.editorInput, { color: theme.text }]}
            multiline
            value={editableText}
            onChangeText={(text) => {
              setEditableText(text);
              if (selection) setSelection(undefined);
            }}
            selection={selection}
            onSelectionChange={(e) => setSelection(e.nativeEvent.selection)}
            placeholder="Type lesson markdown content..."
            placeholderTextColor={theme.subtext}
            textAlignVertical="top"
            autoFocus
          />
        </SafeAreaView>
      </Modal>

      {/* Success Modal */}
      <Modal visible={showSuccessModal} transparent animationType="fade" onRequestClose={() => setShowSuccessModal(false)}>
        <View style={styles.alertBackdrop}>
          <View style={[styles.alertCard, { backgroundColor: theme.background, borderColor: theme.border }]}>
            <View style={[styles.alertIconCircle, { backgroundColor: theme.accent + '20' }]}>
              <FontAwesome5 name="check" size={24} color={theme.accent} />
            </View>
            <Text style={[styles.alertTitle, { color: theme.text }]}>Updated!</Text>
            <Text style={[styles.alertMessage, { color: theme.subtext }]}>
              Lesson content has been saved successfully.
            </Text>
            <TouchableOpacity
              style={[styles.alertButton, { backgroundColor: theme.accent }]}
              onPress={() => setShowSuccessModal(false)}
              activeOpacity={0.8}
            >
              <Text style={styles.alertButtonText}>Done</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  readerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, marginRight: 10 },
  iconShareBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  lessonTitleLarge: { fontSize: 20, fontWeight: '700', lineHeight: 30, textTransform: 'uppercase' },

  /* Horizontal Tab Bar */
  tabContainer: {
    flexDirection: 'row',
    marginHorizontal: 20,
    marginVertical: 12,
    padding: 4,
    borderRadius: 12,
  },
  tabButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 8,
    borderRadius: 8,
    gap: 6,
  },
  activeTab: {
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  tabText: {
    fontSize: 13,
    fontWeight: '600',
  },

  loadingContainer: {
    paddingVertical: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },

  /* Floating Action Button (FAB) */
  fab: {
    position: 'absolute',
    bottom: 90,
    right: 20,
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    zIndex: 99,
  },

  /* Modal Styling */
  modalContainer: { flex: 1 },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  modalTitle: { fontSize: 17, fontWeight: '700' },
  modalCancel: { fontSize: 16 },
  modalSave: { fontSize: 16, fontWeight: '700' },
  editorInput: { flex: 1, padding: 20, fontSize: 15, lineHeight: 22 },

  /* Alert / Confirmation Modal Styling */
  alertBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 30,
  },
  alertCard: {
    width: '100%',
    maxWidth: 320,
    borderRadius: 20,
    padding: 24,
    alignItems: 'center',
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 6,
  },
  alertIconCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  alertTitle: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  alertMessage: {
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 20,
  },
  alertButton: {
    width: '100%',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  alertButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
  cardsContainer: {
    gap: 14,
    paddingVertical: 10,
  },
  card: {
    borderRadius: 14,
    padding: 16,
    borderWidth: 1,
    borderLeftWidth: 1, 
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 10,
    paddingBottom: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(150, 150, 150, 0.2)',
  },
  cardTitle: {
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0.3,
    textTransform: 'capitalize',
  },
});