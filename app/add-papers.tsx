import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './constants/theme';

const FALLBACK_GEMINI_API_KEY = ""; 

// Past Papers Directory & Key File
const PAST_PAPERS_DIR = `${FileSystem.documentDirectory}pastpapers/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

type QuestionStyle = 'MCQ' | 'TF' | 'SA' | 'SEQ' | 'JSON';

export default function AddPastPaper() {
  const [subject, setSubject] = useState('');
  const [term, setTerm] = useState('');
  const [lesson, setLesson] = useState('');
  const [targetStyle, setTargetStyle] = useState<QuestionStyle>('MCQ');
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

  const handleSave = async () => {
    if (!subject.trim() || !term.trim() || !lesson.trim() || !rawText.trim()) {
      showAlert("Error", "Please fill in all metadata fields and provide the question text.", 'error');
      return;
    }

    setIsProcessing(true);

    try {
      let formattedJsonString = '';

      // --- DIRECT JSON PATH (Bypass Gemini API) ---
      if (targetStyle === 'JSON') {
        try {
          const parsed = JSON.parse(rawText.trim());
          formattedJsonString = JSON.stringify(parsed, null, 2);
        } catch {
          throw new Error("Invalid JSON input. Please ensure the text is formatted as valid JSON.");
        }
      } 
      // --- GEMINI AI GENERATION PATH ---
      else {
        // 1. Resolve dynamic API key configuration
        let activeApiKey = FALLBACK_GEMINI_API_KEY;
        try {
          const keyFileCheck = await FileSystem.getInfoAsync(KEY_FILE_URI);
          if (keyFileCheck.exists) {
            const storedKey = await FileSystem.readAsStringAsync(KEY_FILE_URI);
            if (storedKey.trim().length > 0) {
              activeApiKey = storedKey.trim();
            }
          }
        } catch (keyError) {
          console.warn("Could not read local key.txt, relying on default key.", keyError);
        }

        // 2. Select prompt based on target question style
        let prompt = '';
        if (targetStyle === 'MCQ') {
          prompt = `Based on the following source material text, parse and generate multiple choice questions. Each question must have exactly 5 distinct options. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string", "options": ["s1", "s2", "s3", "s4", "s5"], "correct_answer": "string matching one option"}]}.\nSource text:\n${rawText}`;
        } else if (targetStyle === 'TF') {
          prompt = `Based on the following source material text, generate True/False style questions. Each item must contain a header topic text called "question", and an array of exactly 5 distinct conceptual statements related to it. For each statement, provide its corresponding boolean true/false answer value. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string context", "statements": ["s1", "s2", "s3", "s4", "s5"], "answers": [true, false, true, true, false]}]}.\nSource text:\n${rawText}`;
        } else if (targetStyle === 'SA') {
          prompt = `Based on the following source material text, generate clear conceptual short answer questions. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string", "correct_answer": "string", "explanation": "string"}]}.\nSource text:\n${rawText}`;
        } else {
          // Structured Essay Questions (SEQ)
          prompt = `Based on the following source material text, construct Structured Essay Questions (SEQs). Return strictly a JSON object with a key "questions" containing an array. Structure: {"questions": [{"question": "Main Scenario Heading", "sub_questions": [{"sub_question": "string", "marks": 5, "answer_key": "string"}], "model_answer": "string"}]}.\nSource text:\n${rawText}`;
        }

        // 3. Request JSON translation via Gemini API
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: {
              responseMimeType: "application/json"
            }
          })
        });

        const resData = await response.json();
        const generatedJsonText = resData?.candidates?.[0]?.content?.parts?.[0]?.text;

        if (!generatedJsonText) {
          throw new Error("AI failed to structure questions into JSON format.");
        }

        // 4. Validate output JSON integrity
        const parsedJson = JSON.parse(generatedJsonText);
        formattedJsonString = JSON.stringify(parsedJson, null, 2);
      }

      // --- SAVE FILE TO DISK ---
      const fileName = `${targetStyle.toLowerCase()}_${lesson.trim().replace(/\s+/g, '_')}-${subject.trim().replace(/\s+/g, '_')}-${term.trim().replace(/\s+/g, '_')}.json`.toLowerCase();
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

      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 50 }} showsVerticalScrollIndicator={false}>
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

        {/* Question Type Selector */}
        <Text style={[styles.label, { color: theme.accent, marginTop: 10 }]}>Format</Text>
        <View style={styles.styleSelector}>
          {(['MCQ', 'TF', 'SA', 'SEQ', 'JSON'] as QuestionStyle[]).map((style) => (
            <Pressable
              key={style}
              style={[
                styles.styleChip,
                { backgroundColor: theme.card, borderColor: theme.border },
                targetStyle === style && { backgroundColor: theme.buttons, borderColor: theme.accent }
              ]}
              onPress={() => setTargetStyle(style)}
            >
              <Text style={[
                styles.styleChipText, 
                { color: theme.title },
                targetStyle === style && { color: theme.accent, fontWeight: '700' }
              ]}>
                {style}
              </Text>
            </Pressable>
          ))}
        </View>

        <Text style={[styles.label, { color: theme.accent, marginTop: 20 }]}> Question Input</Text>
        <TextInput 
          style={[styles.textArea, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder={targetStyle === 'JSON' ? "Paste structured JSON directly here..." : "Paste past paper raw text here..."}
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
                {targetStyle === 'JSON' ? "Saving JSON..." : "Structuring with AI..."}
              </Text>
            </View>
          ) : (
            <Text style={[styles.submitBtnText, { color: theme.accent }]}>
              {targetStyle === 'JSON' ? "Format & Save JSON" : "Format & Save Paper"}
            </Text>
          )}
        </Pressable>
      </ScrollView>

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
  header: { flexDirection: 'row', alignItems: 'center', padding: 20, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  label: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 },
  input: { height: 54, borderRadius: 16, borderWidth: 1, paddingHorizontal: 16, marginBottom: 15, fontSize: 15, fontWeight: '500' },
  row: { flexDirection: 'row', alignItems: 'center' },
  styleSelector: { flexDirection: 'row', gap: 8, marginBottom: 15 },
  styleChip: { flex: 1, height: 44, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  styleChipText: { fontSize: 12, fontWeight: '600' },
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