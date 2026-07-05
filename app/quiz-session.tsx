import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Modal, Pressable, ScrollView, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';

interface TargetFile {
  filename: string;
  subject: string;
  term: string;
  lesson: string;
}

interface MCQQuestion {
  question: string;
  options: string[];
  correct_answer: string;
}

const GEMINI_API_KEY = "AIzaSyBOsQr74TmAkeTn9V1w1cqXadFm3sKU1BA"; 

// Directory paths
const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;

export default function QuestionSession() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [filesMeta, setFilesMeta] = useState<TargetFile[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  
  const [loading, setLoading] = useState(false);
  const [activeDeck, setActiveDeck] = useState<MCQQuestion[] | null>(null);
  const [selectedAnswers, setSelectedAnswers] = useState<{ [key: number]: string }>({});

  const [subModalVisible, setSubModalVisible] = useState(false);
  const [termModalVisible, setTermModalVisible] = useState(false);

  // Index text source files
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

  const uniqueSubjects = Array.from(new Set(filesMeta.map(f => f.subject)));
  const uniqueTerms = Array.from(new Set(filesMeta.filter(f => f.subject === selectedSubject).map(f => f.term)));
  const availableLessons = filesMeta.filter(f => f.subject === selectedSubject && f.term === selectedTerm);

  // Helper function to handle offline/error fallback generation
  const handleFallbackQuiz = async () => {
    try {
      const cacheCheck = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!cacheCheck.exists) {
        throw new Error("No cached questions directory found.");
      }

      const cachedFiles = await FileSystem.readDirectoryAsync(CACHE_DIR);
      const jsonFiles = cachedFiles.filter(file => file.endsWith('.json'));

      if (jsonFiles.length === 0) {
        throw new Error("Cache is empty.");
      }

      let aggregatedQuestions: MCQQuestion[] = [];

      // Read all cached quizzes and pool their questions together
      for (const file of jsonFiles) {
        const fileContent = await FileSystem.readAsStringAsync(`${CACHE_DIR}${file}`);
        const parsed = JSON.parse(fileContent);
        if (Array.isArray(parsed)) {
          aggregatedQuestions = [...aggregatedQuestions, ...parsed];
        } else if (parsed && Array.isArray(parsed.questions)) {
          aggregatedQuestions = [...aggregatedQuestions, ...parsed.questions];
        }
      }

      if (aggregatedQuestions.length === 0) {
        throw new Error("No valid questions found in local cache files.");
      }

      // Shuffle and select up to 5 random questions from pool
      const shuffled = [...aggregatedQuestions].sort(() => 0.5 - Math.random());
      const fallbackDeck = shuffled.slice(0, 5);

      setActiveDeck(fallbackDeck);
      setSelectedAnswers({});

      Alert.alert(
        "Offline Mode Activated",
        "Unable to reach the AI Engine. A personalized fallback review has been generated using your offline question vault."
      );
    } catch (fallbackError) {
      console.error(fallbackError);
      Alert.alert(
        "Connection & Storage Error", 
        "Failed to reach the AI engine, and no offline question vaults were found. Please check your internet connection."
      );
    }
  };

  const launchDeck = async (filename: string) => {
    setLoading(true);
    try {
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
      
      const prompt = `Based on the following source material text, generate exactly 5 multiple choice questions. Each question must have exactly 5 distinct options. Return the data strictly as a JSON object containing an array called "questions". Each item in the array must contain "question" (string), "options" (array of 5 strings), and "correct_answer" (string matching exactly one of the options).
Source material text:${targetStr}`;

      const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`, {
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
      
      if (!resData?.candidates?.[0]?.content?.parts?.[0]?.text) {
        throw new Error("Invalid structure received from Gemini API endpoint.");
      }

      const rawJsonText = resData.candidates[0].content.parts[0].text;
      const parsedQuiz = JSON.parse(rawJsonText);
      let targetDeck: MCQQuestion[] = [];

      if (parsedQuiz && Array.isArray(parsedQuiz.questions)) {
        targetDeck = parsedQuiz.questions;
      } else if (Array.isArray(parsedQuiz)) {
        targetDeck = parsedQuiz;
      } else {
        throw new Error("JSON parsed successfully but 'questions' array was not found.");
      }

      setActiveDeck(targetDeck);
      setSelectedAnswers({});

      // Ensure the caching directory exists, then save the response locally
      const jsonFilename = filename.replace('.txt', '.json');
      const dirInfo = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(CACHE_DIR, { intermediates: true });
      }
      await FileSystem.writeAsStringAsync(`${CACHE_DIR}${jsonFilename}`, JSON.stringify(targetDeck));

    } catch (e) { 
      console.warn("API Call failed, attempting fallback to local question vault...", e);
      await handleFallbackQuiz();
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.title }]}>Curriculum Review</Text>
      </View>

      {/* SINGLE LINE DROPDOWN HEADERS */}
      {!activeDeck && !loading && (
        <View style={styles.dropdownRow}>
          <Pressable 
            style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} 
            onPress={() => setSubModalVisible(true)}
          >
            <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>
              {selectedSubject ?? "Select Subject"}
            </Text>
            <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
          </Pressable>

          <Pressable 
            onPress={() => { if(selectedSubject) setTermModalVisible(true); }}
            style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border, opacity: selectedSubject ? 1 : 0.5 }]}
          >
            <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>
              {selectedTerm ?? "Select Term"}
            </Text>
            <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
          </Pressable>
        </View>
      )}

      {loading ? (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={theme.accent} />
          <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>Compiling Engine Questions...</Text>
        </View>
      ) : (
        <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
          {!activeDeck ? (
            <View style={{ marginTop: 5 }}>
              {selectedSubject && selectedTerm ? (
                <View>
                  <Text style={[styles.label, { color: theme.accent }]}>Available Modules</Text>
                  {availableLessons.length === 0 ? (
                    <Text style={{ color: theme.subtext, fontStyle: 'italic', marginLeft: 5 }}>No lessons match this combination.</Text>
                  ) : (
                    availableLessons.map(les => (
                      <Pressable 
                        key={les.filename} 
                        style={[styles.lessonRow, { backgroundColor: theme.card, borderColor: theme.border }]}
                        onPress={() => launchDeck(les.filename)}
                      >
                        <View style={{ flex: 1 }}>
                          <Text style={[styles.lessonTitle, { color: theme.title }]}>{les.lesson}</Text>
                          <Text style={{ color: theme.subtext, fontSize: 12 }}>{les.subject} • {les.term}</Text>
                        </View>
                        <FontAwesome5 name="chevron-right" size={14} color={theme.border} />
                      </Pressable>
                    ))
                  )}
                </View>
              ) : (
                <View style={styles.placeholderContainer}>
                  <FontAwesome5 name="hand-pointer" size={35} color={theme.border} style={{ marginBottom: 12 }} />
                  <Text style={{ color: theme.subtext, textAlign: 'center', fontWeight: '500' }}>
                    Choose a subject and term above to display available modules.
                  </Text>
                </View>
              )}
            </View>
          ) : (
            <View>
              <Pressable style={styles.exitRow} onPress={() => setActiveDeck(null)}>
                <FontAwesome5 name="arrow-left" size={14} color={theme.accent} />
                <Text style={[styles.exitText, { color: theme.accent }]}>Change Module</Text>
              </Pressable>

              {activeDeck.map((item, qIdx) => {
                const chosen = selectedAnswers[qIdx];
                return (
                  <View key={qIdx} style={[styles.quizCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
                    <Text style={[styles.quizQuestion, { color: theme.title }]}>{qIdx + 1}. {item.question}</Text>
                    
                    {item.options.map((option, oIdx) => {
                      const isSelected = chosen === option;
                      const isCorrect = option === item.correct_answer;
                      
                      let optionBg = 'transparent';
                      let optionBorder = theme.border;

                      if (chosen) {
                        if (isCorrect) {
                          optionBg = 'rgba(74, 222, 128, 0.15)'; 
                          optionBorder = '#4ade80';
                        } else if (isSelected) {
                          optionBg = 'rgba(248, 113, 113, 0.15)'; 
                          optionBorder = '#f87171';
                        }
                      } else if (isSelected) {
                        optionBg = theme.background;
                      }

                      return (
                        <Pressable
                          key={oIdx}
                          disabled={!!chosen}
                          style={[styles.optionButton, { backgroundColor: optionBg, borderColor: optionBorder }]}
                          onPress={() => setSelectedAnswers(p => ({ ...p, [qIdx]: option }))}
                        >
                          <Text style={[styles.optionText, { color: theme.title, fontWeight: isSelected ? '700' : '400' }]}>
                            {option}
                          </Text>
                          {chosen && isCorrect && <FontAwesome5 name="check-circle" size={14} color="#4ade80" />}
                          {chosen && isSelected && !isCorrect && <FontAwesome5 name="times-circle" size={14} color="#f87171" />}
                        </Pressable>
                      );
                    })}
                  </View>
                );
              })}
            </View>
          )}
        </ScrollView>
      )}

      {/* MODAL PICKER COMPONENTS */}
      <Modal visible={subModalVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setSubModalVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Subject</Text>
            <ScrollView style={{ width: '100%', maxHeight: 280 }}>
              {uniqueSubjects.map(sub => (
                <Pressable key={sub} style={styles.modalItem} onPress={() => { setSelectedSubject(sub); setSelectedTerm(null); setSubModalVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '600' }}>{sub}</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>

      <Modal visible={termModalVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setTermModalVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Term</Text>
            <ScrollView style={{ width: '100%', maxHeight: 280 }}>
              {uniqueTerms.map(t => (
                <Pressable key={t} style={styles.modalItem} onPress={() => { setSelectedTerm(t); setTermModalVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '600' }}>{t}</Text>
                </Pressable>
              ))}
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
  header: { paddingHorizontal: 25, paddingTop: 25, paddingBottom: 10 },
  title: { fontSize: 26, fontWeight: '800', letterSpacing: -0.5 },
  dropdownRow: { flexDirection: 'row', paddingHorizontal: 25, marginBottom: 20, gap: 12 },
  dropdown: { flex: 1, height: 46, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 14 },
  dropdownText: { fontSize: 14, fontWeight: '600', flex: 1, marginRight: 8 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  label: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 15 },
  placeholderContainer: { alignItems: 'center', marginTop: 40, paddingHorizontal: 20 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  lessonRow: { flexDirection: 'row', alignItems: 'center', padding: 18, borderRadius: 20, borderWidth: 1, marginBottom: 10 },
  lessonTitle: { fontSize: 16, fontWeight: '700', marginBottom: 2 },
  exitRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 20 },
  exitText: { fontSize: 14, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', borderRadius: 24, padding: 20, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 15 },
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' },
  quizCard: { padding: 20, borderRadius: 22, borderWidth: 1, marginBottom: 16 },
  quizQuestion: { fontSize: 16, fontWeight: '700', marginBottom: 15, lineHeight: 22 },
  optionButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
  optionText: { fontSize: 14, flex: 1, paddingRight: 10 }
});