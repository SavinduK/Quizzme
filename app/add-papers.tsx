import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  useColorScheme,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './constants/theme';

const FALLBACK_GEMINI_API_KEY = ""; 

// Past Papers Directory & Settings File
const PAST_PAPERS_DIR = `${FileSystem.documentDirectory}pastpapers/`;
const SETTINGS_FILE_URI = `${FileSystem.documentDirectory}settings.json`;

type FormatMode = 'AUTO' | 'JSON';

interface ApiKeyItem {
  id: string;
  label: string;
  key: string;
}

interface SettingsData {
  apiKeys?: ApiKeyItem[];
  activeKeyId?: string;
  selectedModel?: string;
  qCount?: number;
  qStyle?: string;
  customPrompt?: string;
}

export default function AddPastPaper() {
  const [subject, setSubject] = useState('');
  const [term, setTerm] = useState('');
  const [lesson, setLesson] = useState('');
  const [formatMode, setFormatMode] = useState<FormatMode>('AUTO');
  const [rawText, setRawText] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // Custom Alert Modal State
  const [alertVisible, setAlertVisible] = useState(false);
  const [alertData, setAlertData] = useState({ title: '', message: '', type: 'success', onDismiss: () => {} });

  const showAlert = (title: string, message: string, type: 'success' | 'error' = 'success', onDismiss = () => {}) => {
    setAlertData({ title, message, type, onDismiss });
    setAlertVisible(true);
  };

  // Helper to load active key and selected model settings from JSON
  const getActiveSettings = async (): Promise<{ apiKey: string; model: string }> => {
    let apiKey = FALLBACK_GEMINI_API_KEY;
    let model = 'gemini-2.5-flash';

    try {
      const fileCheck = await FileSystem.getInfoAsync(SETTINGS_FILE_URI);
      if (fileCheck.exists) {
        const rawJson = await FileSystem.readAsStringAsync(SETTINGS_FILE_URI);
        const settings: SettingsData = JSON.parse(rawJson);

        if (settings.selectedModel) {
          model = settings.selectedModel;
        }

        if (settings.apiKeys && settings.apiKeys.length > 0) {
          const activeKeyObj = settings.apiKeys.find((k) => k.id === settings.activeKeyId);
          if (activeKeyObj && activeKeyObj.key.trim()) {
            apiKey = activeKeyObj.key.trim();
          } else if (settings.apiKeys[0].key.trim()) {
            // Fallback to the first available key if no active ID match is found
            apiKey = settings.apiKeys[0].key.trim();
          }
        }
      }
    } catch (keyError) {
      console.warn("Could not read settings.json, relying on fallback configurations.", keyError);
    }

    return { apiKey, model };
  };

  // Utility to clean markdown wrappers or preambles from AI outputs
  const cleanJsonText = (raw: string): string => {
    let cleaned = raw.trim();
    // Strip markdown code fences if present
    if (cleaned.startsWith('```')) {
      cleaned = cleaned.replace(/^```(?:json)?\n?/, '').replace(/\n?```$/, '');
    }
    // Isolate pure JSON string between outer braces/brackets
    const firstBrace = cleaned.indexOf('{');
    const lastBrace = cleaned.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
      cleaned = cleaned.substring(firstBrace, lastBrace + 1);
    }
    return cleaned.trim();
  };

  const handleSave = async () => {
    if (!subject.trim() || !term.trim() || !lesson.trim() || !rawText.trim()) {
      showAlert("Error", "Please fill in all metadata fields and provide the question text.", 'error');
      return;
    }

    setIsProcessing(true);

    try {
      let formattedJsonString = '';

      // --- DIRECT JSON PATH ---
      if (formatMode === 'JSON') {
        try {
          const cleanedText = cleanJsonText(rawText);
          const parsed = JSON.parse(cleanedText);
          formattedJsonString = JSON.stringify(parsed, null, 2);
        } catch {
          throw new Error("Invalid JSON input. Please check syntax or remove syntax errors.");
        }
      } 
      // --- GEMINI AI AUTO-DETECT GENERATION PATH ---
      else {
        // 1. Resolve dynamic API key and selected model from settings.json
        const { apiKey: activeApiKey, model: selectedModel } = await getActiveSettings();

        if (!activeApiKey) {
          throw new Error("No API Key found. Please add an API Key in Configuration Settings.");
        }

        const systemPrompt = `Analyze the provided past paper text and classify each question into its appropriate format by looking into the way the answers are given:
      - "MCQ" (Multiple Choice)
      - "TF" (True/False with multiple statements)
      - "SA" (Short Answer)
      - "SEQ" (Structured Essay)

      Extract and return a JSON object with a single key "questions" containing an array of classified questions. 

      Target Schemas per question type:
      1. MCQ:
        { "type": "MCQ", "question": "string", "options": ["opt1", "opt2", "opt3", "opt4", "opt5"], "correct_answer": "exact_matching_option_string", "explanation": "string" }
      2. TF:
        { "type": "TF", "question": "topic context string", "statements": ["s1", "s2", "s3", "s4", "s5"], "answers": [true, false, true, true, false], "explanation": "string" }
      3. SA:
        { "type": "SA", "question": "string", "correct_answer": "string", "explanation": "string" }
      4. SEQ:
        { "type": "SEQ", "question": "Main Topic", "sub_questions": [{"sub_question": "string", "marks": 5, "answer_key": "string"}], "model_answer": "string" }`;

        const prompt = `${systemPrompt}\n\nPast Paper Content Text:\n${rawText}`;

        const response = await fetch(
          `[https://generativelanguage.googleapis.com/v1beta/models/$](https://generativelanguage.googleapis.com/v1beta/models/$){selectedModel}:generateContent?key=${activeApiKey}`,
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

        // Check for explicit API error response
        if (resData.error) {
          throw new Error(`API Error (${resData.error.code}): ${resData.error.message}`);
        }

        const rawModelOutput = resData?.candidates?.[0]?.content?.parts?.[0]?.text;

        if (!rawModelOutput) {
          throw new Error("No response returned from the AI model.");
        }

        // Clean and parse JSON payload
        const cleanedJsonText = cleanJsonText(rawModelOutput);
        const parsedJson = JSON.parse(cleanedJsonText);
        formattedJsonString = JSON.stringify(parsedJson, null, 2);
      }

      // --- SAVE FILE TO DISK ---
      const fileName = `paper_${lesson.trim().replace(/\s+/g, '_')}-${subject.trim().replace(/\s+/g, '_')}-${term.trim().replace(/\s+/g, '_')}.json`.toLowerCase();
      const fileUri = `${PAST_PAPERS_DIR}${fileName}`;

      const folderInfo = await FileSystem.getInfoAsync(PAST_PAPERS_DIR);
      if (!folderInfo.exists) {
        await FileSystem.makeDirectoryAsync(PAST_PAPERS_DIR, { intermediates: true });
      }

      await FileSystem.writeAsStringAsync(fileUri, formattedJsonString);

      showAlert("Success", "Past paper questions formatted and saved successfully.", 'success', () => {
        router.back();
      });
    } catch (error: any) {
      console.error(error);
      showAlert("Save Error", error?.message || "Failed to process and format past paper questions.", 'error');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleModalClose = () => {
    setAlertVisible(false);
    if (alertData.onDismiss) {
      alertData.onDismiss();
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Pressable style={[styles.backBtn, { backgroundColor: theme.card }]} onPress={() => router.back()}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Add Past Paper</Text>
      </View>

      <KeyboardAvoidingView
        style={styles.keyboardView}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0}
      >
        <ScrollView 
          style={styles.scroll} 
          contentContainerStyle={{ paddingBottom: 50 }} 
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          <Text style={[styles.label, { color: theme.accent }]}>Paper Data</Text>
          
          <TextInput 
            style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
            placeholder="Subject Name (e.g., Physiology)"
            placeholderTextColor={theme.subtext}
            value={subject}
            onChangeText={setSubject}
          />
          <TextInput 
            style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
            placeholder="Term / Year (e.g., 2023 Batch)"
            placeholderTextColor={theme.subtext}
            value={term}
            onChangeText={setTerm}
          />
          <TextInput 
            style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
            placeholder="Lesson / Topic (e.g., Cardiovascular)"
            placeholderTextColor={theme.subtext}
            value={lesson}
            onChangeText={setLesson}
          />

          {/* Mode Selector */}
          <Text style={[styles.label, { color: theme.accent, marginTop: 10 }]}>Format Mode</Text>
          <View style={styles.styleSelector}>
            <Pressable
              style={[
                styles.styleChip,
                { backgroundColor: theme.card, borderColor: theme.border },
                formatMode === 'AUTO' && { backgroundColor: theme.buttons, borderColor: theme.accent }
              ]}
              onPress={() => setFormatMode('AUTO')}
            >
              <Text style={[
                styles.styleChipText, 
                { color: theme.title },
                formatMode === 'AUTO' && { color: theme.accent, fontWeight: '700' }
              ]}>
                AI Auto-Detect (Text)
              </Text>
            </Pressable>

            <Pressable
              style={[
                styles.styleChip,
                { backgroundColor: theme.card, borderColor: theme.border },
                formatMode === 'JSON' && { backgroundColor: theme.buttons, borderColor: theme.accent }
              ]}
              onPress={() => setFormatMode('JSON')}
            >
              <Text style={[
                styles.styleChipText, 
                { color: theme.title },
                formatMode === 'JSON' && { color: theme.accent, fontWeight: '700' }
              ]}>
                Raw JSON
              </Text>
            </Pressable>
          </View>

          <Text style={[styles.label, { color: theme.accent, marginTop: 20 }]}>Question Input</Text>
          <TextInput 
            style={[styles.textArea, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
            placeholder={formatMode === 'JSON' ? "Paste structured JSON directly here..." : "Paste raw past paper text here (MCQs, T/F, Short Answers, or SEQs)..."}
            placeholderTextColor={theme.subtext}
            multiline
            numberOfLines={10}
            textAlignVertical="top"
            value={rawText}
            onChangeText={setRawText}
          />

          <Pressable 
            disabled={isProcessing}
            style={[styles.submitBtn, { backgroundColor: theme.buttons, opacity: isProcessing ? 0.6 : 1, borderColor: theme.accent, borderWidth: 1 }]} 
            onPress={handleSave}
          >
            {isProcessing ? (
              <View style={styles.row}>
                <ActivityIndicator size="small" color={theme.accent} style={{ marginRight: 10 }} />
                <Text style={[styles.submitBtnText, { color: theme.accent }]}>
                  {formatMode === 'JSON' ? "Saving JSON..." : "Detecting & Structuring..."}
                </Text>
              </View>
            ) : (
              <Text style={[styles.submitBtnText, { color: theme.accent }]}>
                {formatMode === 'JSON' ? "Format & Save JSON" : "Process & Save Paper"}
              </Text>
            )}
          </Pressable>
        </ScrollView>
      </KeyboardAvoidingView>

      {/* CUSTOM THEME ALERT MODAL */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={alertVisible}
        onRequestClose={handleModalClose}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.background, borderColor: theme.border }]}>
            <View style={styles.modalHeaderRow}>
              <FontAwesome5 
                name={alertData.type === 'success' ? "check-circle" : "exclamation-circle"} 
                size={20} 
                color={alertData.type === 'success' ? "#4BB543" : "#D8000C"} 
                style={{ marginRight: 10 }}
              />
              <Text style={[styles.modalTitle, { color: theme.title }]}>{alertData.title}</Text>
            </View>
            <Text style={[styles.modalMessage, { color: theme.subtext }]}>{alertData.message}</Text>
            
            <Pressable 
              style={[styles.modalCloseBtn, { backgroundColor: theme.buttons, borderColor: theme.accent }]} 
              onPress={handleModalClose}
            >
              <Text style={[styles.modalCloseBtnText, { color: theme.accent }]}>Dismiss</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  keyboardView: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 20, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  label: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 },
  input: { height: 54, borderRadius: 16, borderWidth: 1, paddingHorizontal: 16, marginBottom: 15, fontSize: 15, fontWeight: '500' },
  row: { flexDirection: 'row', alignItems: 'center' },
  styleSelector: { flexDirection: 'row', gap: 10, marginBottom: 15 },
  styleChip: { flex: 1, height: 48, borderRadius: 14, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  styleChipText: { fontSize: 13, fontWeight: '600' },
  textArea: { minHeight: 220, borderRadius: 20, borderWidth: 1, padding: 16, fontSize: 13, fontFamily: 'monospace', marginBottom: 25 },
  submitBtn: { height: 56, borderRadius: 20, justifyContent: 'center', alignItems: 'center', marginTop: 10 },
  submitBtnText: { fontSize: 16, fontWeight: '700' },

  // Custom Modal Styling
  modalOverlay: { 
    flex: 1, 
    backgroundColor: 'rgba(0,0,0,0.4)', 
    justifyContent: 'center', 
    alignItems: 'center',
    paddingHorizontal: 30
  },
  modalContent: { 
    width: '100%', 
    borderRadius: 20, 
    borderWidth: 1, 
    padding: 24, 
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5
  },
  modalHeaderRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
  modalTitle: { fontSize: 18, fontWeight: '800' },
  modalMessage: { fontSize: 14, lineHeight: 20, marginBottom: 24 },
  modalCloseBtn: { height: 44, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  modalCloseBtnText: { fontWeight: '700', fontSize: 14 }
});