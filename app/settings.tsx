import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, Modal, Pressable, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import Header from './components/header';
import { Colors } from './constants/theme';

const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function Settings() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [apiKey, setApiKey] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);
  const [qCount, setQCount] = useState<number>(5);
  // Extended type definition to include 'SA' (Short Answer)
  const [qStyle, setQStyle] = useState<'MCQ' | 'TF' | 'SA'>('MCQ');
  const [customPrompt, setCustomPrompt] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  // States to manage section collapsibility
  const [aiExpanded, setAiExpanded] = useState(false);
  const [quizExpanded, setQuizExpanded] = useState(true);

  // Custom Alert Modal State
  const [alertVisible, setAlertVisible] = useState(false);
  const [alertData, setAlertData] = useState({ title: '', message: '', type: 'success' });

  const showAlert = (title: string, message: string, type: 'success' | 'error' = 'success') => {
    setAlertData({ title, message, type });
    setAlertVisible(true);
  };

  // Load configuration parsing lines from key.txt
  const loadSettings = async () => {
    try {
      const fileInfo = await FileSystem.getInfoAsync(KEY_FILE_URI);
      if (fileInfo.exists) {
        const lines = (await FileSystem.readAsStringAsync(KEY_FILE_URI)).split('\n');
        if (lines[0]) setApiKey(lines[0].trim());
        if (lines[1]) setQCount(parseInt(lines[1].trim(), 10) || 5);
        if (lines[2]) {
          const loadedStyle = lines[2].trim().toUpperCase();
          if (loadedStyle === 'TF') setQStyle('TF');
          else if (loadedStyle === 'SA') setQStyle('SA');
          else setQStyle('MCQ');
        }
        if (lines[3]) setCustomPrompt(lines[3].trim());
      }
    } catch (e) {
      console.error("Failed to load settings:", e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, [])
  );

  const handleSaveSettings = async () => {
    setIsSaving(true);
    try {
      const cleanKey = apiKey.trim();
      const cleanPrompt = customPrompt.trim();
      const configPayload = `${cleanKey}\n${qCount}\n${qStyle}\n${cleanPrompt}`;
      await FileSystem.writeAsStringAsync(KEY_FILE_URI, configPayload);
      
      showAlert("Success", "Configuration preferences updated successfully.", 'success');
    } catch (e) {
      console.error("Failed to save preferences:", e);
      showAlert("Error", "Could not save configurations locally.", 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <Header title='Configuration Settings'/>

      <View style={styles.content}>
        
        {/* SECTION 1: AI ENGINE CREDENTIALS */}
        <Pressable 
          style={styles.accordionHeader} 
          onPress={() => setAiExpanded(!aiExpanded)}
        >
          <Text style={[styles.sectionTitle, { color: theme.accent }]}>AI settings</Text>
          <FontAwesome5 
            name={aiExpanded ? "chevron-up" : "chevron-down"} 
            size={12} 
            color={theme.accent} 
          />
        </Pressable>

        {aiExpanded && (
          <View style={styles.configCard}>
            <View style={styles.labelRow}>
              <Text style={[styles.inputLabel, { color: theme.title }]}>Gemini API Key</Text>
            </View>

            <View style={[styles.inputContainer, { borderColor: theme.border, backgroundColor: theme.background }]}>
              <TextInput
                style={[styles.flexInput, { color: theme.title }]}
                placeholder="Paste your Gemini API key here"
                placeholderTextColor={theme.subtext}
                value={apiKey}
                onChangeText={setApiKey}
                autoCapitalize="none"
                autoCorrect={false}
                secureTextEntry={!showApiKey} 
              />
              <Pressable 
                onPress={() => setShowApiKey(!showApiKey)} 
                style={styles.eyeBtn}
                hitSlop={8}
              >
                <FontAwesome5 name={showApiKey ? "eye" : "eye-slash"} size={15} color={theme.subtext} />
              </Pressable>
            </View>
          </View>
        )}

        {/* SECTION SEPARATOR HORIZONTAL BAR */}
        <View style={[styles.horizontalBar, { backgroundColor: theme.border }]} />

        {/* SECTION 2: QUIZ GENERATION SETTINGS */}
        <Pressable 
          style={styles.accordionHeader} 
          onPress={() => setQuizExpanded(!quizExpanded)}
        >
          <Text style={[styles.sectionTitle, { color: theme.accent }]}>Quiz Settings</Text>
          <FontAwesome5 
            name={quizExpanded ? "chevron-up" : "chevron-down"} 
            size={12} 
            color={theme.accent} 
          />
        </Pressable>

        {quizExpanded && (
          <View style={styles.configCard}>
            
            {/* QUESTION COUNT SELECTOR */}
            <View style={styles.settingRow}>
              <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>Number of Questions</Text>
              <View style={styles.buttonOptionRow}>
                {[5, 10, 15, 20].map((num) => (
                  <Pressable
                    key={num}
                    style={[
                      styles.normalOptionBtn, 
                      styles.flexButton,
                      { borderColor: theme.border },
                      qCount === num && { backgroundColor: theme.buttons, borderColor: theme.accent }
                    ]}
                    onPress={() => setQCount(num)}
                  >
                    <Text style={[styles.normalOptionText, { color: qCount === num ? '#fff' : theme.title }]}>
                      {num}
                    </Text>
                  </Pressable>
                ))}
              </View>
            </View>

            {/* QUESTION STYLE SELECTOR (Updated with Short Answer button option) */}
            <View style={styles.settingRow}>
              <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>Question Type</Text>
              <View style={styles.buttonOptionRow}>
                <Pressable
                  style={[
                    styles.normalOptionBtn, 
                    styles.flexButton,
                    { borderColor: theme.border },
                    qStyle === 'MCQ' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                  ]}
                  onPress={() => setQStyle('MCQ')}
                >
                  <Text style={[styles.normalOptionText, { color: qStyle === 'MCQ' ? '#fff' : theme.title }]}>MCQ</Text>
                </Pressable>
                
                <Pressable
                  style={[
                    styles.normalOptionBtn, 
                    styles.flexButton,
                    { borderColor: theme.border },
                    qStyle === 'TF' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                  ]}
                  onPress={() => setQStyle('TF')}
                >
                  <Text style={[styles.normalOptionText, { color: qStyle === 'TF' ? '#fff' : theme.title }]}>True / False</Text>
                </Pressable>

                <Pressable
                  style={[
                    styles.normalOptionBtn, 
                    styles.flexButton,
                    { borderColor: theme.border },
                    qStyle === 'SA' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                  ]}
                  onPress={() => setQStyle('SA')}
                >
                  <Text style={[styles.normalOptionText, { color: qStyle === 'SA' ? '#fff' : theme.title }]}>Short Ans</Text>
                </Pressable>
              </View>
            </View>

            {/* CUSTOM SYSTEM PROMPT */}
            <View style={styles.settingRow}>
              <View style={[styles.labelRow, { marginBottom: 8 }]}>
                <Text style={[styles.inputLabel, { color: theme.title }]}>Custom Generation Instructions</Text>
              </View>
              <TextInput
                style={[styles.textArea, { color: theme.title, borderColor: theme.border, backgroundColor: theme.background }]}
                placeholder="e.g., Focus heavily on clinical diagnostics..."
                placeholderTextColor={theme.subtext}
                value={customPrompt}
                onChangeText={setCustomPrompt}
                multiline={true}
                numberOfLines={3}
                textAlignVertical="top"
              />
            </View>

            <Pressable 
              style={[styles.saveBtn, { backgroundColor: theme.buttons, opacity: isSaving ? 0.7 : 1 ,borderColor:theme.accent,borderWidth:1}]} 
              onPress={handleSaveSettings}
              disabled={isSaving}
            >
              {isSaving ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <>
                  <FontAwesome5 name="save" size={14} color={theme.accent} style={{ marginRight: 8 }} />
                  <Text style={[styles.saveBtnText,{color:theme.accent}]}>Save Configurations</Text>
                </>
              )}
            </Pressable>
          </View>
        
        )}
      </View>
      <Footer/>

      {/* THEME MATCHING CUSTOM MODAL FOR ALERTS */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={alertVisible}
        onRequestClose={() => setAlertVisible(false)}
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
              onPress={() => setAlertVisible(false)}
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
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 25, paddingTop: 25, paddingBottom: 25 },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  content: { flex: 1, paddingHorizontal: 25,paddingVertical:20 },
  accordionHeader: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    paddingVertical: 10,
    paddingRight: 5
  },
  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginLeft: 5 },
  horizontalBar: { height: 1, width: '100%', marginVertical: 14, opacity: 0.6 },
  configCard: { paddingVertical: 10, paddingHorizontal: 5, gap: 16 },
  settingRow: { gap: 4 },
  labelRow: { flexDirection: 'row', alignItems: 'center' },
  inputLabel: { fontSize: 14, fontWeight: '700', letterSpacing: -0.1 },
  inputContainer: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    height: 48, 
    borderRadius: 14, 
    borderWidth: 1, 
    paddingLeft: 16, 
    overflow: 'hidden',
    marginTop: 8
  },
  flexInput: { flex: 1, height: '100%', fontSize: 14 },
  eyeBtn: { height: '100%', paddingHorizontal: 16, justifyContent: 'center', alignItems: 'center' },
  textArea: { minHeight: 80, borderRadius: 14, borderWidth: 1, padding: 14, fontSize: 14, lineHeight: 20 },
  buttonOptionRow: { 
    flexDirection: 'row', 
    gap: 8, 
    marginTop: 4, 
    width: '100%' 
  },
  flexButton: {
    flex: 1
  },
  normalOptionBtn: { 
    height: 48, 
    borderRadius: 14, 
    borderWidth: 1, 
    flexDirection: 'row', 
    alignItems: 'center', 
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.01)'
  },
  normalOptionText: { fontWeight: '600', fontSize: 14 },
  saveBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 12 },
  saveBtnText: { fontWeight: '700', fontSize: 15 },
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