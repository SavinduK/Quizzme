import { FontAwesome5 } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import * as ImagePicker from 'expo-image-picker';
import { useLocalSearchParams, useRouter } from 'expo-router';
import * as Sharing from 'expo-sharing';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  TouchableWithoutFeedback,
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
const PAST_PAPERS_DIR = `${FileSystem.documentDirectory}pastpapers/`;
const HANDWRITTEN_DIR = `${FileSystem.documentDirectory}handwritten_notes/`;
const SETTINGS_FILE_URI = `${FileSystem.documentDirectory}settings.json`;
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

interface ApiKeyConfig {
  id: string;
  label: string;
  key: string;
}

interface SettingsConfig {
  apiKeys?: ApiKeyConfig[];
  activeKeyId?: string;
  selectedModel?: string;
  qCount?: number | string;
  qStyle?: string;
  customPrompt?: string;
}

/* ---------------- Auto-Scaling Handwritten Image Component ---------------- */
function ScaledHandwrittenImage({ uri, onDelete, theme }: { uri: string; onDelete: () => void; theme: any }) {
  const [aspectRatio, setAspectRatio] = useState<number>(1);

  useEffect(() => {
    Image.getSize(
      uri,
      (width, height) => {
        if (width && height) {
          setAspectRatio(width / height);
        }
      },
      (error) => console.warn('Could not scale image:', error)
    );
  }, [uri]);

  return (
    <View style={[styles.handwrittenCard, { borderColor: theme.border, backgroundColor: theme.card ?? theme.buttons }]}>
      <Image
        source={{ uri }}
        style={[styles.handwrittenImage, { aspectRatio }]}
        resizeMode="contain"
      />
      <TouchableOpacity style={styles.deleteNoteBtn} onPress={onDelete}>
        <FontAwesome5 name="trash-alt" size={14} color="#FF453A" />
      </TouchableOpacity>
    </View>
  );
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
        id: `block-${acc.length}`,
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
  const params = useLocalSearchParams<{ filename: string; lesson: string; initialTab?: 'notes' | 'quiz' | 'summary' | 'past_paper' | 'handwritten' }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [loading, setLoading] = useState(true);
  const [readingContent, setReadingContent] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'notes' | 'quiz' | 'summary' | 'past_paper' | 'handwritten'>(params.initialTab || 'notes');

  // Handwritten Image Notes State
  const [handwrittenImages, setHandwrittenImages] = useState<string[]>([]);
  const [loadingHandwritten, setLoadingHandwritten] = useState<boolean>(false);

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

  // Past Paper State
  const [pastPaperDeck, setPastPaperDeck] = useState<QuizQuestion[] | null>(null);
  const [pastPaperInputText, setPastPaperInputText] = useState<string>('');
  const [isProcessingPastPaper, setIsProcessingPastPaper] = useState<boolean>(false);
  const [showPastPaperMenu, setShowPastPaperMenu] = useState<boolean>(false);
  const [showDeleteConfirmModal, setShowDeleteConfirmModal] = useState<boolean>(false);

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
        await loadSavedPastPaper();
        await loadHandwrittenNotes();
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

  /* ---------------- Handwritten Notes Logic ---------------- */
  const getLessonNotesDir = () => {
    if (!params.filename) return '';
    const cleanName = params.filename.replace(/\.[^/.]+$/, "");
    return `${HANDWRITTEN_DIR}${cleanName}/`;
  };

  const loadHandwrittenNotes = async () => {
    if (!params.filename) return;
    try {
      setLoadingHandwritten(true);
      const lessonNotesFolder = getLessonNotesDir();
      const folderInfo = await FileSystem.getInfoAsync(lessonNotesFolder);
      if (folderInfo.exists) {
        const files = await FileSystem.readDirectoryAsync(lessonNotesFolder);
        const imageUris = files.map(file => `${lessonNotesFolder}${file}`);
        setHandwrittenImages(imageUris.reverse());
      }
    } catch (e) {
      console.warn("Could not load handwritten notes:", e);
    } finally {
      setLoadingHandwritten(false);
    }
  };

  const savePickedImage = async (uri: string) => {
    try {
      const lessonNotesFolder = getLessonNotesDir();
      const folderInfo = await FileSystem.getInfoAsync(lessonNotesFolder);
      if (!folderInfo.exists) {
        await FileSystem.makeDirectoryAsync(lessonNotesFolder, { intermediates: true });
      }

      const fileName = `note_${Date.now()}.jpg`;
      const targetPath = `${lessonNotesFolder}${fileName}`;
      await FileSystem.copyAsync({ from: uri, to: targetPath });

      setHandwrittenImages(prev => [targetPath, ...prev]);
    } catch (e) {
      console.error(e);
      Alert.alert("Error", "Could not save photo note.");
    }
  };

  const handleTakePhoto = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert("Permission required", "Camera permission is required to take photo notes.");
      return;
    }

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]?.uri) {
      await savePickedImage(result.assets[0].uri);
    }
  };

  const handlePickGalleryImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      Alert.alert("Permission required", "Gallery permission is required to import notes.");
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });

    if (!result.canceled && result.assets[0]?.uri) {
      await savePickedImage(result.assets[0].uri);
    }
  };

  const handleDeleteHandwrittenNote = async (imageUri: string) => {
    Alert.alert("Delete Note", "Are you sure you want to delete this handwritten note?", [
      { text: "Cancel", style: "cancel" },
      {
        text: "Delete",
        style: "destructive",
        onPress: async () => {
          try {
            await FileSystem.deleteAsync(imageUri, { idempotent: true });
            setHandwrittenImages(prev => prev.filter(img => img !== imageUri));
          } catch (e) {
            Alert.alert("Error", "Failed to delete handwritten image note.");
          }
        },
      },
    ]);
  };

  /* ---------------- Past Paper & Quiz Logic ---------------- */
  const getPastPaperFileUri = () => {
    if (!params.filename) return '';
    const jsonFileName = params.filename.replace(/\.[^/.]+$/, "") + ".json";
    return `${PAST_PAPERS_DIR}${jsonFileName}`;
  };

  const loadSavedPastPaper = async () => {
    if (!params.filename) return;
    try {
      const pastPaperUri = getPastPaperFileUri();
      const fileInfo = await FileSystem.getInfoAsync(pastPaperUri);
      
      if (fileInfo.exists) {
        const rawJson = await FileSystem.readAsStringAsync(pastPaperUri);
        const parsed = JSON.parse(rawJson);
        const loadedDeck: QuizQuestion[] = parsed.questions || (Array.isArray(parsed) ? parsed : [parsed]);

        if (loadedDeck[0]?.statements) setIsTFQuiz(true);
        else if (loadedDeck[0]?.sub_questions) setIsSEQQuiz(true);
        else if (loadedDeck[0]?.options) setIsTFQuiz(false);
        else setIsSAQuiz(true);

        setPastPaperDeck(loadedDeck);
      }
    } catch (e) {
      console.warn("Could not load saved past paper:", e);
    }
  };

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
      UTI: 'net.daringfireball.markdown',
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
    let customPrompt = '';
    let selectedModel = 'gemini-2.5-flash';

    try {
      const settingsCheck = await FileSystem.getInfoAsync(SETTINGS_FILE_URI);
      if (settingsCheck.exists) {
        const rawJson = await FileSystem.readAsStringAsync(SETTINGS_FILE_URI);
        const settings: SettingsConfig = JSON.parse(rawJson);
        if (Array.isArray(settings.apiKeys) && settings.activeKeyId) {
          const activeObj = settings.apiKeys.find((k) => k.id === settings.activeKeyId);
          if (activeObj?.key) {
            activeApiKey = activeObj.key;
          }
        }
        if (settings.selectedModel) {
          selectedModel = settings.selectedModel;
        }
        if (settings.qCount !== undefined) {
          targetCount = typeof settings.qCount === 'number' ? settings.qCount : parseInt(settings.qCount, 10) || 5;
        }
        if (settings.qStyle) {
          const parsedStyle = settings.qStyle.trim().toUpperCase();
          if (['TF', 'SA', 'SEQ', 'MCQ'].includes(parsedStyle)) {
            targetStyle = parsedStyle;
          }
        }
        if (settings.customPrompt) {
          customPrompt = settings.customPrompt;
        }
      }
    } catch (e) {
      console.warn("Using default key settings.", e);
    }
    return { activeApiKey, targetCount, targetStyle, customPrompt, selectedModel };
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
      const { activeApiKey, targetCount, targetStyle, customPrompt, selectedModel } = await fetchApiKeyConfig();

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

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${selectedModel}:generateContent?key=${activeApiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { responseMimeType: "application/json" }
          })
        }
      );

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

  const handleSaveDirectJson = async () => {
    if (!pastPaperInputText.trim()) {
      Alert.alert("Error", "Text input cannot be empty.");
      return;
    }
    try {
      const parsed = JSON.parse(pastPaperInputText);
      const targetQuestions: QuizQuestion[] = parsed.questions || (Array.isArray(parsed) ? parsed : [parsed]);
      
      const dirInfo = await FileSystem.getInfoAsync(PAST_PAPERS_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(PAST_PAPERS_DIR, { intermediates: true });
      }

      const targetPath = getPastPaperFileUri();
      const jsonContentToSave = JSON.stringify({ questions: targetQuestions }, null, 2);
      await FileSystem.writeAsStringAsync(targetPath, jsonContentToSave);

      if (targetQuestions[0]?.statements) setIsTFQuiz(true);
      else if (targetQuestions[0]?.sub_questions) setIsSEQQuiz(true);
      else if (targetQuestions[0]?.options) setIsTFQuiz(false);
      else setIsSAQuiz(true);

      setPastPaperDeck(targetQuestions);
      setCurrentQuestionIdx(0);
      setRunningScore(0);
      setQuizFinished(false);
      resetQuestionStates();
      Alert.alert("Saved", "Past paper questions saved to memory!");
    } catch (e) {
      Alert.alert("Invalid JSON", "Could not parse formatted JSON. Ensure your string follows standard structure.");
    }
  };

  const handleConvertPlaintextWithGemini = async () => {
    if (!pastPaperInputText.trim()) {
      Alert.alert("Error", "Text input cannot be empty.");
      return;
    }
    
    setIsProcessingPastPaper(true);

    try {
      const { activeApiKey, customPrompt, selectedModel } = await fetchApiKeyConfig();

      const systemPrompt = `Analyze the provided past paper text and classify each question into its appropriate format by looking into the way the answers are given:
- "MCQ" (Multiple Choice)
- "TF" (True/False with multiple statements)
- "SA" (Short Answer)
- "SEQ" (Structured Essay)

Extract and return a JSON object with a single key "questions" containing an array of classified questions.`;

      const prompt = `${systemPrompt}\n${customPrompt || ""}\n\nPast Paper Content Text:\n${pastPaperInputText}`;

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${selectedModel}:generateContent?key=${activeApiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { responseMimeType: "application/json" }
          })
        }
      );

      const resData = await response.json();
      
      if (!resData.candidates?.[0]?.content?.parts?.[0]?.text) {
        throw new Error("Invalid response structural format received from Gemini.");
      }

      const rawJsonText = resData.candidates[0].content.parts[0].text;
      
      const dirInfo = await FileSystem.getInfoAsync(PAST_PAPERS_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(PAST_PAPERS_DIR, { intermediates: true });
      }
      const targetPath = getPastPaperFileUri();
      await FileSystem.writeAsStringAsync(targetPath, rawJsonText);

      const parsed = JSON.parse(rawJsonText);
      const targetDeck: QuizQuestion[] = parsed.questions || parsed;

      setPastPaperDeck(targetDeck);
      setCurrentQuestionIdx(0);
      setRunningScore(0);
      setQuizFinished(false);
      resetQuestionStates();
      
      Alert.alert("Conversion Successful", "Past Paper auto-detected and saved to memory.");
    } catch (e) {
      console.error(e);
      Alert.alert("Error", "Could not process text via Gemini AI.");
    } finally {
      setIsProcessingPastPaper(false);
    }
  };

  const handleClearPastPaper = async () => {
    try {
      const pastPaperUri = getPastPaperFileUri();
      const fileInfo = await FileSystem.getInfoAsync(pastPaperUri);
      if (fileInfo.exists) {
        await FileSystem.deleteAsync(pastPaperUri, { idempotent: true });
      }
      setPastPaperDeck(null);
      setPastPaperInputText('');
      setShowDeleteConfirmModal(false);
    } catch (e) {
      console.warn("Error deleting past paper file:", e);
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
      const summaryDirCheck = await FileSystem.getInfoAsync(SUMMARY_DIR);
      if (!summaryDirCheck.exists) {
        await FileSystem.makeDirectoryAsync(SUMMARY_DIR, { intermediates: true });
      }

      const summaryCheck = await FileSystem.getInfoAsync(summaryFileUri);
      if (summaryCheck.exists) {
        const cachedSummary = await FileSystem.readAsStringAsync(summaryFileUri);
        setSummaryContent(cachedSummary);
        setLoadingSummary(false);
        return;
      }

      setSummaryStatus("Reading source content...");
      const sourceText = readingContent || await FileSystem.readAsStringAsync(sourceFileUri);

      setSummaryStatus("Connecting to AI engine...");
      const { activeApiKey } = await fetchApiKeyConfig();

      const prompt = `You are a world-class academic summarizer. Read the following source text and provide a comprehensive, clear, and highly structured summary. Use Markdown features natively: clear Headings (# and ##), bulleted takeaway lists, bold terms for critical definitions, and short focused paragraphs.\n\nSource Text:\n${sourceText}`;

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      });

      const resData = await response.json();
      const rawSummaryMarkdown = resData.candidates[0].content.parts[0].text;

      await FileSystem.writeAsStringAsync(summaryFileUri, rawSummaryMarkdown);
      setSummaryContent(rawSummaryMarkdown);

    } catch (error) {
      console.error("Summary processing failure:", error);
      Alert.alert("Generation Failed", "Could not structure a summary. Please check your network connection.");
    } finally {
      setLoadingSummary(false);
    }
  };

  const handleTabPress = (tab: 'notes' | 'quiz' | 'summary' | 'past_paper' | 'handwritten') => {
    setActiveTab(tab);
    if (tab === 'quiz' && !activeDeck && !loadingQuiz) {
      launchDeck();
    } else if (tab === 'summary' && !summaryContent && !loadingSummary) {
      fetchOrGenerateSummary();
    } else if (tab === 'past_paper' && !pastPaperDeck) {
      loadSavedPastPaper();
    } else if (tab === 'handwritten') {
      loadHandwrittenNotes();
    }
  };

  const handleNextQuestion = () => {
    const currentActiveDeck = activeTab === 'past_paper' ? pastPaperDeck : activeDeck;
    if (!currentActiveDeck) return;
    if (currentQuestionIdx + 1 < currentActiveDeck.length) {
      setCurrentQuestionIdx(p => p + 1);
      resetQuestionStates();
    } else {
      setQuizFinished(true);
    }
  };

  const evaluateTfQuestion = () => {
    const currentActiveDeck = activeTab === 'past_paper' ? pastPaperDeck : activeDeck;
    if (!currentActiveDeck) return;
    const currentQ = currentActiveDeck[currentQuestionIdx];
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

  const currentDeckRef = activeTab === 'past_paper' ? pastPaperDeck : activeDeck;
  const maxPossibleScore = currentDeckRef
    ? (isTFQuiz ? currentDeckRef.length * 5 : (isSEQQuiz ? currentDeckRef.length * 10 : currentDeckRef.length))
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

          <TouchableOpacity style={[styles.iconShareBtn]} onPress={copyToClipboard}>
            <FontAwesome5 name="copy" size={16} color={theme.accent} />
          </TouchableOpacity>

          <TouchableOpacity style={[styles.iconShareBtn]} onPress={shareAsMarkdown}>
            <FontAwesome5 name="share-alt" size={16} color={theme.accent} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Horizontal Tab Navigation */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabScrollWrapper}>
        <View style={[styles.tabContainer, { backgroundColor: theme.buttons ?? '#f2f2f2' }]}>
          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'notes' && [styles.activeTab, { backgroundColor: theme.background }]]}
            onPress={() => handleTabPress('notes')}
          >
            <FontAwesome5 name="book-open" size={12} color={activeTab === 'notes' ? theme.accent : theme.subtext} />
            <Text style={[styles.tabText, { color: activeTab === 'notes' ? theme.accent : theme.subtext }]}>Notes</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'handwritten' && [styles.activeTab, { backgroundColor: theme.background }]]}
            onPress={() => handleTabPress('handwritten')}
          >
            <FontAwesome5 name="camera" size={12} color={activeTab === 'handwritten' ? theme.accent : theme.subtext} />
            <Text style={[styles.tabText, { color: activeTab === 'handwritten' ? theme.accent : theme.subtext }]}>Handwritten</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'quiz' && [styles.activeTab, { backgroundColor: theme.background }]]}
            onPress={() => handleTabPress('quiz')}
          >
            <FontAwesome5 name="bolt" size={12} color={activeTab === 'quiz' ? theme.accent : theme.subtext} />
            <Text style={[styles.tabText, { color: activeTab === 'quiz' ? theme.accent : theme.subtext }]}>Quiz</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'summary' && [styles.activeTab, { backgroundColor: theme.background }]]}
            onPress={() => handleTabPress('summary')}
          >
            <FontAwesome5 name="file-alt" size={12} color={activeTab === 'summary' ? theme.accent : theme.subtext} />
            <Text style={[styles.tabText, { color: activeTab === 'summary' ? theme.accent : theme.subtext }]}>Summary</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.tabButton, activeTab === 'past_paper' && [styles.activeTab, { backgroundColor: theme.background }]]}
            onPress={() => handleTabPress('past_paper')}
          >
            <FontAwesome5 name="history" size={12} color={activeTab === 'past_paper' ? theme.accent : theme.subtext} />
            <Text style={[styles.tabText, { color: activeTab === 'past_paper' ? theme.accent : theme.subtext }]}>Past Paper</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>

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

        {/* Handwritten Image Notes View Component */}
        {activeTab === 'handwritten' && (
          <View style={styles.handwrittenContainer}>
            <Text style={[styles.pastPaperTitleText, { color: theme.text, marginBottom: 12 }]}>
              Handwritten Notes
            </Text>

            {/* Action Buttons to Take/Pick Photo */}
            <View style={styles.handwrittenBtnRow}>
              <TouchableOpacity
                style={[styles.handwrittenActionBtn, { backgroundColor: theme.accent }]}
                onPress={handleTakePhoto}
              >
                <FontAwesome5 name="camera" size={14} color="#FFF" style={{ marginRight: 6 }} />
                <Text style={styles.btnText}>Take Photo</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.handwrittenActionBtn, { backgroundColor: theme.buttons, borderWidth: 1, borderColor: theme.border }]}
                onPress={handlePickGalleryImage}
              >
                <FontAwesome5 name="image" size={14} color={theme.text} style={{ marginRight: 6 }} />
                <Text style={[styles.btnText, { color: theme.text }]}>Import Photo</Text>
              </TouchableOpacity>
            </View>

            {/* Scroll View displaying dynamically scaled images */}
            {loadingHandwritten ? (
              <ActivityIndicator size="large" color={theme.accent} style={{ marginTop: 20 }} />
            ) : handwrittenImages.length === 0 ? (
              <View style={styles.emptyContainer}>
                <FontAwesome5 name="file-image" size={40} color={theme.subtext} />
                <Text style={{ color: theme.subtext, marginTop: 12, textAlign: 'center' }}>
                  No handwritten photo notes attached yet. Take or import a photo to keep notes attached to this lesson.
                </Text>
              </View>
            ) : (
              <ScrollView style={{ width: '100%' }} nestedScrollEnabled showsVerticalScrollIndicator={false}>
                {handwrittenImages.map((uri, idx) => (
                  <ScaledHandwrittenImage
                    key={idx}
                    uri={uri}
                    theme={theme}
                    onDelete={() => handleDeleteHandwrittenNote(uri)}
                  />
                ))}
              </ScrollView>
            )}
          </View>
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
              parseMarkdownToCards(summaryContent).map((block) => (
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

        {activeTab === 'past_paper' && (
          <View>
            <View style={styles.pastPaperHeaderRow}>
              <Text style={[styles.pastPaperTitleText, { color: theme.text }]}>
                Past Paper Questions
              </Text>
              
              {pastPaperDeck && pastPaperDeck.length > 0 && (
                <View style={{ position: 'relative', zIndex: 10 }}>
                  <TouchableOpacity
                    style={styles.menuIconBtn}
                    onPress={() => setShowPastPaperMenu(!showPastPaperMenu)}
                    hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                  >
                    <FontAwesome5 name="ellipsis-v" size={16} color={theme.text} />
                  </TouchableOpacity>

                  {showPastPaperMenu && (
                    <View style={[styles.dropdownMenu, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}>
                      <TouchableOpacity
                        style={styles.dropdownMenuItem}
                        onPress={() => {
                          setShowPastPaperMenu(false);
                          setShowDeleteConfirmModal(true);
                        }}
                      >
                        <FontAwesome5 name="trash-alt" size={14} color="#FF453A" style={{ marginRight: 8 }} />
                        <Text style={[styles.dropdownMenuText, { color: "#FF453A" }]}>Delete Past Paper</Text>
                      </TouchableOpacity>
                    </View>
                  )}
                </View>
              )}
            </View>

            {!pastPaperDeck || pastPaperDeck.length === 0 ? (
              <View style={styles.pastPaperInputContainer}>
                <TextInput
                  style={[styles.pastPaperInput, { color: theme.text, borderColor: theme.border, backgroundColor: theme.buttons }]}
                  multiline
                  numberOfLines={10}
                  placeholder="Paste JSON or plain text here..."
                  placeholderTextColor={theme.subtext}
                  value={pastPaperInputText}
                  onChangeText={setPastPaperInputText}
                  textAlignVertical="top"
                />

                {isProcessingPastPaper ? (
                  <ActivityIndicator size="large" color={theme.accent} style={{ marginTop: 20 }} />
                ) : (
                  <View style={styles.pastPaperBtnRow}>
                    <TouchableOpacity
                      style={[styles.pastPaperActionBtn, { backgroundColor: theme.accent }]}
                      onPress={handleSaveDirectJson}
                    >
                      <Text style={styles.btnText}>Save JSON Directly</Text>
                    </TouchableOpacity>

                    <TouchableOpacity
                      style={[styles.pastPaperActionBtn, { backgroundColor: theme.accent }]}
                      onPress={handleConvertPlaintextWithGemini}
                    >
                      <Text style={styles.btnText}>Convert Plaintext via AI</Text>
                    </TouchableOpacity>
                  </View>
                )}
              </View>
            ) : (
              <View>
                {quizFinished ? (
                  <QuizResults
                    runningScore={runningScore}
                    maxPossibleScore={maxPossibleScore}
                    onReturn={() => {
                      setCurrentQuestionIdx(0);
                      setRunningScore(0);
                      setQuizFinished(false);
                      resetQuestionStates();
                    }}
                  />
                ) : (
                  <QuizCard
                    item={pastPaperDeck[currentQuestionIdx]}
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
                    totalQuestions={pastPaperDeck.length}
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

      {/* Delete Confirmation Popup Modal */}
      <Modal visible={showDeleteConfirmModal} transparent animationType="fade" onRequestClose={() => setShowDeleteConfirmModal(false)}>
        <TouchableWithoutFeedback onPress={() => setShowDeleteConfirmModal(false)}>
          <View style={styles.alertBackdrop}>
            <TouchableWithoutFeedback>
              <View style={[styles.alertCard, { backgroundColor: theme.background, borderColor: theme.border }]}>
                <View style={[styles.alertIconCircle, { backgroundColor: '#FF453A20' }]}>
                  <FontAwesome5 name="trash-alt" size={22} color="#FF453A" />
                </View>
                <Text style={[styles.alertTitle, { color: theme.text }]}>Delete Past Paper?</Text>
                <Text style={[styles.alertMessage, { color: theme.subtext }]}>
                  Are you sure you want to delete this past paper from device memory? This action cannot be undone.
                </Text>
                <View style={styles.dialogBtnRow}>
                  <TouchableOpacity
                    style={[styles.dialogBtn, { backgroundColor: theme.buttons }]}
                    onPress={() => setShowDeleteConfirmModal(false)}
                    activeOpacity={0.8}
                  >
                    <Text style={[styles.dialogBtnText, { color: theme.text }]}>Cancel</Text>
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={[styles.dialogBtn, { backgroundColor: '#FF453A' }]}
                    onPress={handleClearPastPaper}
                    activeOpacity={0.8}
                  >
                    <Text style={[styles.dialogBtnText, { color: '#FFFFFF' }]}>Delete</Text>
                  </TouchableOpacity>
                </View>
              </View>
            </TouchableWithoutFeedback>
          </View>
        </TouchableWithoutFeedback>
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
  tabScrollWrapper: {
    maxHeight: 50,
    marginVertical: 10,
  },
  tabContainer: {
    flexDirection: 'row',
    marginHorizontal: 15,
    padding: 4,
    borderRadius: 12,
  },
  tabButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 8,
    paddingHorizontal: 14,
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
    fontSize: 11,
    fontWeight: '600',
  },

  loadingContainer: {
    paddingVertical: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },

  /* Handwritten Tab Styles */
  handwrittenContainer: {
    paddingVertical: 10,
  },
  handwrittenBtnRow: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
  },
  handwrittenActionBtn: {
    flex: 1,
    flexDirection: 'row',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  handwrittenCard: {
    position: 'relative',
    marginBottom: 16,
    borderRadius: 12,
    borderWidth: 1,
    overflow: 'hidden',
  },
  handwrittenImage: {
    width: '100%',
  },
  deleteNoteBtn: {
    position: 'absolute',
    top: 10,
    right: 10,
    backgroundColor: 'rgba(0, 0, 0, 0.4)',
    padding: 8,
    borderRadius: 20,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
    paddingHorizontal: 20,
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
  alertTitle: { fontSize: 20, fontWeight: '700', marginBottom: 8 },
  alertMessage: { fontSize: 14, textAlign: 'center', lineHeight: 20, marginBottom: 20 },
  alertButton: { width: '100%', paddingVertical: 12, borderRadius: 12, alignItems: 'center' },
  alertButtonText: { color: '#FFFFFF', fontSize: 15, fontWeight: '700' },
  dialogBtnRow: { flexDirection: 'row', gap: 12, width: '100%' },
  dialogBtn: { flex: 1, paddingVertical: 12, borderRadius: 12, alignItems: 'center' },
  dialogBtnText: { fontSize: 15, fontWeight: '700' },

  cardsContainer: { gap: 14, paddingVertical: 10 },
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
  cardTitle: { fontSize: 15, fontWeight: '700', letterSpacing: 0.3, textTransform: 'capitalize' },

  /* Past Paper Specific Styles */
  pastPaperHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 12,
  },
  pastPaperTitleText: { fontSize: 18, fontWeight: '700' },
  menuIconBtn: { padding: 6, borderRadius: 8 },
  dropdownMenu: {
    position: 'absolute',
    top: 30,
    right: 0,
    width: 170,
    borderRadius: 12,
    borderWidth: 1,
    paddingVertical: 6,
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 5,
  },
  dropdownMenuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  dropdownMenuText: { fontSize: 14, fontWeight: '600' },

  pastPaperInputContainer: { paddingVertical: 5 },
  pastPaperInput: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    fontSize: 14,
    minHeight: 180,
  },
  pastPaperBtnRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 15,
  },
  pastPaperActionBtn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: { color: '#FFFFFF', fontWeight: '700', fontSize: 13 },
});