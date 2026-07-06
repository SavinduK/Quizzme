import { FontAwesome5 } from '@expo/vector-icons';
import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

const FALLBACK_GEMINI_API_KEY = ""; 

// Directory and File paths
const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function AddQuestions() {
  const [subject, setSubject] = useState('');
  const [term, setTerm] = useState('');
  const [lesson, setLesson] = useState('');
  const [plainText, setPlainText] = useState(''); 
  const [processingPdf, setProcessingPdf] = useState(false);
  const [attachedFileName, setAttachedFileName] = useState<string | null>(null);
  
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // Requests document picking permissions, reads binary array values, and streams data directly to Gemini
  const handlePdfUpload = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
      });

      if (result.canceled || !result.assets || result.assets.length === 0) return;

      const pickedFile = result.assets[0];
      setAttachedFileName(pickedFile.name);
      setProcessingPdf(true);

      // 1. Resolve dynamic API Key configuration from file storage
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
        console.warn("Could not read local key.txt, relying on default token assignment.", keyError);
      }

      // 2. Convert local temporary cache URI into standard base64 chunk strings
      const base64Data = await FileSystem.readAsStringAsync(pickedFile.uri, {
        encoding: FileSystem.EncodingType.Base64,
      });

      const prompt = "Extract and structure all core study and technical informational notes found inside this document cleanly. Omit conversational introductions. Produce clean, highly informative plain text summaries.";

      // 3. Transmit base64 inline structures alongside active resolved key configuration
      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{
            parts: [
              { text: prompt },
              {
                inlineData: {
                  mimeType: "application/pdf",
                  data: base64Data
                }
              }
            ]
          }]
        })
      });

      const resData = await response.json();
      const extractedText = resData?.candidates?.[0]?.content?.parts?.[0]?.text;

      if (!extractedText) {
        throw new Error("AI engine failed to parse structure or layout inside document asset parameters.");
      }

      setPlainText(extractedText);
      Alert.alert("Success", "PDF text extracted and populated successfully.");
    } catch (error) {
      console.error(error);
      Alert.alert("Extraction Error", "Failed to parse information from the selected PDF. Please check your network or try again.");
      setAttachedFileName(null);
    } finally {
      setProcessingPdf(false);
    }
  };

  const handleSave = async () => {
    if (!subject.trim() || !term.trim() || !lesson.trim() || !plainText.trim()) {
      Alert.alert("Error", "Please fill in all meta fields and provide the questions data.");
      return;
    }

    try {
      const fileName = `${lesson.trim().replace(/\s+/g, '_')}-${subject.trim().replace(/\s+/g, '_')}-${term.trim().replace(/\s+/g, '_')}.txt`.toLowerCase();
      const fileUri = `${QUESTIONS_DIR}${fileName}`;

      const folderInfo = await FileSystem.getInfoAsync(QUESTIONS_DIR);
      if (!folderInfo.exists) {
        await FileSystem.makeDirectoryAsync(QUESTIONS_DIR, { intermediates: true });
      }

      await FileSystem.writeAsStringAsync(fileUri, plainText);
      
      Alert.alert("Success", "Question engine files compiled perfectly.", [
        { text: "OK", onPress: () => router.back() }
      ]);
    } catch (error) {
      Alert.alert("Save Error", "Failed to write data to disk.");
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Pressable style={[styles.backBtn, { backgroundColor: theme.card }]} onPress={() => router.back()}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Import Engine Data</Text>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 50 }} showsVerticalScrollIndicator={false}>
        <Text style={[styles.label, { color: theme.accent }]}>Metadata Configuration</Text>
        
        <TextInput 
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Subject Name (e.g., Physiology)"
          placeholderTextColor={theme.subtext}
          value={subject}
          onChangeText={setSubject}
        />
        <TextInput 
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Term Name (e.g., Term 2)"
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

        <Text style={[styles.label, { color: theme.accent, marginTop: 15 }]}>Document Ingestion</Text>
        
        <Pressable 
          disabled={processingPdf}
          style={[styles.pdfBtn, { backgroundColor: theme.card, borderColor: theme.accent }]} 
          onPress={handlePdfUpload}
        >
          {processingPdf ? (
            <View style={styles.row}>
              <ActivityIndicator size="small" color={theme.accent} style={{ marginRight: 10 }} />
              <Text style={{ color: theme.title, fontWeight: '600' }}>AI Ingestion Tool Processing...</Text>
            </View>
          ) : (
            <View style={styles.row}>
              <FontAwesome5 name="file-pdf" size={18} color={theme.accent} style={{ marginRight: 10 }} />
              <Text style={{ color: theme.title, fontWeight: '600' }}>
                {attachedFileName ? `Change: ${attachedFileName}` : "Upload Source Material PDF"}
              </Text>
            </View>
          )}
        </Pressable>

        <Text style={[styles.label, { color: theme.accent, marginTop: 20 }]}>Source Material Text Content</Text>
        <TextInput 
          style={[styles.textArea, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Extracted data will fill here automatically, or paste your schematic framework manually..."
          placeholderTextColor={theme.subtext}
          multiline
          numberOfLines={12}
          textAlignVertical="top"
          value={plainText}
          onChangeText={setPlainText}
        />

        <Pressable 
          disabled={processingPdf}
          style={[styles.submitBtn, { backgroundColor: theme.accent, opacity: processingPdf ? 0.6 : 1 }]} 
          onPress={handleSave}
        >
          <Text style={styles.submitBtnText}>Compile & Commit File</Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 25, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  label: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 },
  input: { height: 54, borderRadius: 16, borderWidth: 1, paddingHorizontal: 16, marginBottom: 15, fontSize: 15, fontWeight: '500' },
  pdfBtn: { height: 54, borderRadius: 16, borderWidth: 1, borderStyle: 'dashed', justifyContent: 'center', alignItems: 'center', marginBottom: 15 },
  row: { flexDirection: 'row', alignItems: 'center' },
  textArea: { minHeight: 220, borderRadius: 20, borderWidth: 1, padding: 16, fontSize: 13, fontFamily: 'monospace', marginBottom: 25 },
  submitBtn: { height: 56, borderRadius: 20, justifyContent: 'center', alignItems: 'center', marginTop: 10 },
  submitBtnText: { color: 'white', fontSize: 16, fontWeight: '700' }
});