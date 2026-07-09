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
  statements?: string[];
  answers?: boolean[];
}

const FALLBACK_GEMINI_API_KEY = ""; 

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function QuestionSession() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [filesMeta, setFilesMeta] = useState<TargetFile[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  
  const [loading, setLoading] = useState(false);
  const [activeDeck, setActiveDeck] = useState<MCQQuestion[] | null>(null);
  const [isTFQuiz, setIsTFQuiz] = useState<boolean>(false);
  
  // Single Question Display & Quiz State Indexes
  const [currentQuestionIdx, setCurrentQuestionIdx] = useState<number>(0);
  const [runningScore, setRunningScore] = useState<number>(0);
  const [quizFinished, setQuizFinished] = useState<boolean>(false);
  
  // Selection trackers
  const [chosenAnswer, setChosenAnswer] = useState<string | null>(null);
  const [tfSelections, setTfSelections] = useState<{ [key: number]: boolean | null }>({0: null, 1: null, 2: null, 3: null, 4: null});
  const [tfChecked, setTfChecked] = useState<boolean>(false);
  const [tfQuestionScore, setTfQuestionScore] = useState<number>(0); 

  const [subModalVisible, setSubModalVisible] = useState(false);
  const [termModalVisible, setTermModalVisible] = useState(false);
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

  const uniqueSubjects = Array.from(new Set(filesMeta.map(f => f.subject)));
  const uniqueTerms = Array.from(new Set(filesMeta.filter(f => f.subject === selectedSubject).map(f => f.term)));
  const availableLessons = filesMeta.filter(f => f.subject === selectedSubject && f.term === selectedTerm);

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

  const launchDeck = async (filename: string) => {
    setLoading(true);
    setCurrentQuestionIdx(0);
    setRunningScore(0);
    setQuizFinished(false);
    setChosenAnswer(null);
    setTfSelections({0: null, 1: null, 2: null, 3: null, 4: null});
    setTfChecked(false);
    setTfQuestionScore(0);

    const jsonCacheFilename = filename.replace('.txt', '.json');
    const specificCacheUri = `${CACHE_DIR}${jsonCacheFilename}`;

    try {
      let activeApiKey = FALLBACK_GEMINI_API_KEY;
      let targetCount = 5;
      let targetStyle = 'MCQ';

      try {
        const keyFileCheck = await FileSystem.getInfoAsync(KEY_FILE_URI);
        if (keyFileCheck.exists) {
          const lines = (await FileSystem.readAsStringAsync(KEY_FILE_URI)).split('\n');
          if (lines[0]) activeApiKey = lines[0].trim();
          if (lines[1]) targetCount = parseInt(lines[1].trim(), 10) || 5;
          if (lines[2]) targetStyle = lines[2].trim() === 'TF' ? 'TF' : 'MCQ';
        }
      } catch (keyError) {
        console.warn("Relying on default configurations.", keyError);
      }

      setIsTFQuiz(targetStyle === 'TF');
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
      
      let prompt = "";
      if (targetStyle === 'MCQ') {
        prompt = `Based on the following source material text, generate exactly ${targetCount} multiple choice questions. Each question must have exactly 5 distinct options. Return the data strictly as a JSON object containing an array called "questions". Each item in the array must contain "question" (string), "options" (array of 5 strings), and "correct_answer" (string matching exactly one of the options).\nSource material text:${targetStr}`;
      } else {
        prompt = `Based on the following source material text, generate exactly ${targetCount} True/False style questions. Each item must contain a header topic text called "question", and an array of exactly 5 distinct conceptual statements related to it. For each statement, provide its corresponding boolean true/false answer value. Return data strictly as a JSON object containing an array called "questions". Structure: {"questions": [{"question": "string context", "statements": ["s1", "s2", "s3", "s4", "s5"], "answers": [true, false, true, true, false]}]}.\nSource material text:${targetStr}`;
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
      
      // Ensure Cache Directory Exists and save locally
      const cacheDirCheck = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!cacheDirCheck.exists) {
        await FileSystem.makeDirectoryAsync(CACHE_DIR, { intermediates: true });
      }
      await FileSystem.writeAsStringAsync(specificCacheUri, rawJsonText);

      const parsedQuiz = JSON.parse(rawJsonText);
      let targetDeck: MCQQuestion[] = parsedQuiz.questions || parsedQuiz;
      setActiveDeck(targetDeck);

    } catch (e) { 
      console.warn("Online generation failed or device offline. Checking workspace cache alternatives...", e);
      
      try {
        const localCacheCheck = await FileSystem.getInfoAsync(specificCacheUri);
        if (localCacheCheck.exists) {
          const rawCachedText = await FileSystem.readAsStringAsync(specificCacheUri);
          const parsedCache = JSON.parse(rawCachedText);
          let targetDeck: MCQQuestion[] = parsedCache.questions || parsedCache;
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

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.title }]}>Curriculum Review</Text>
      </View>

      {!activeDeck && !loading && (
        <View style={styles.dropdownRow}>
          <Pressable style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} onPress={() => setSubModalVisible(true)}>
            <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>{selectedSubject ?? "Select Subject"}</Text>
            <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
          </Pressable>
          <Pressable onPress={() => { if(selectedSubject) setTermModalVisible(true); }} style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border, opacity: selectedSubject ? 1 : 0.5 }]}>
            <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>{selectedTerm ?? "Select Term"}</Text>
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
                    <Text style={{ color: theme.subtext, fontStyle: 'italic', marginLeft: 5 }}>No lessons found.</Text>
                  ) : (
                    availableLessons.map(les => (
                      <View key={les.filename} style={[styles.lessonRowContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
                        <Pressable style={styles.lessonPressable} onPress={() => launchDeck(les.filename)}>
                          <View style={{ flex: 1 }}>
                            <Text style={[styles.lessonTitle, { color: theme.title }]}>{les.lesson}</Text>
                            <Text style={{ color: theme.subtext, fontSize: 12 }}>{les.subject} • {les.term}</Text>
                          </View>
                          <FontAwesome5 name="chevron-right" size={14} color={theme.border} />
                        </Pressable>
                        <Pressable style={styles.deleteBtn} onPress={() => { setTargetFilename(les.filename); setDeleteModalVisible(true); }}>
                          <FontAwesome5 name="trash" size={14} color={theme.delete} />
                        </Pressable>
                      </View>
                    ))
                  )}
                </View>
              ) : (
                <View style={styles.placeholderContainer}>
                  <FontAwesome5 name="hand-pointer" size={35} color={theme.border} style={{ marginBottom: 12 }} />
                  <Text style={{ color: theme.subtext, textAlign: 'center', fontWeight: '500' }}>Choose a subject and term above to display modules.</Text>
                </View>
              )}
            </View>
          ) : quizFinished ? (
            <View style={[styles.resultsCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <View style={[styles.iconCircle, { backgroundColor: 'rgba(74, 222, 128, 0.15)' }]}>
                <FontAwesome5 name="trophy" size={36} color="#4ade80" />
              </View>
              <Text style={[styles.resultsTitle, { color: theme.title }]}>Quiz Complete!</Text>
              <Text style={[styles.resultsSubtitle, { color: theme.subtext }]}>You have successfully reviewed this architecture segment.</Text>
              
              <View style={[styles.finalScoreBox, { backgroundColor: theme.background }]}>
                <Text style={[styles.finalScoreLabel, { color: theme.subtext }]}>TOTAL MARKS EARNED</Text>
                <Text style={[styles.finalScoreText, { color: theme.accent }]}>{runningScore} / {maxPossibleScore}</Text>
                <Text style={{ color: theme.subtext, fontSize: 12, fontWeight: '600', marginTop: 4 }}>
                  Accuracy Rate: {maxPossibleScore > 0 ? Math.round((runningScore / maxPossibleScore) * 100) : 0}%
                </Text>
              </View>

              <Pressable 
                style={[styles.submitActionBtn, { backgroundColor: theme.accent, width: '100%' }]} 
                onPress={() => setActiveDeck(null)}
              >
                <FontAwesome5 name="home" size={14} color="white" style={{ marginRight: 8 }} />
                <Text style={styles.submitActionBtnText}>Return to Modules</Text>
              </Pressable>
            </View>
          ) : (
            <View>
              <View style={styles.quizHeaderRow}>
                <Pressable style={styles.exitRow} onPress={() => setActiveDeck(null)}>
                  <FontAwesome5 name="arrow-left" size={12} color={theme.accent} />
                  <Text style={[styles.exitText, { color: theme.accent }]}>Exit Quiz</Text>
                </Pressable>
                
                <View style={[styles.scoreBadge, { backgroundColor: theme.accent }]}>
                  <Text style={styles.scoreBadgeText}>Total Score: {runningScore} / {maxPossibleScore}</Text>
                </View>
              </View>

              <Text style={styles.progressText}>Question {currentQuestionIdx + 1} of {activeDeck.length}</Text>

              {(() => {
                const item = activeDeck[currentQuestionIdx];
                const isTFStyle = Array.isArray(item.statements);

                return (
                  <View style={[styles.quizCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
                    <Text style={[styles.quizQuestion, { color: theme.title }]}>{item.question}</Text>
                    
                    {/* RENDER STANDARD MCQ INTERFACE */}
                    {!isTFStyle && item.options.map((option, oIdx) => {
                      const isSelected = chosenAnswer === option;
                      const isCorrect = option === item.correct_answer;
                      let optionBg = 'transparent';
                      let optionBorder = theme.border;

                      if (chosenAnswer) {
                        if (isCorrect) {
                          optionBg = 'rgba(74, 222, 128, 0.15)'; 
                          optionBorder = '#4ade80';
                        } else if (isSelected) {
                          optionBg = 'rgba(248, 113, 113, 0.15)'; 
                          optionBorder = '#f87171';
                        }
                      }

                      return (
                        <Pressable
                          key={oIdx}
                          style={[styles.optionButton, { backgroundColor: optionBg, borderColor: optionBorder }]}
                          onPress={() => {
                            if (chosenAnswer === option) {
                              // Deselecting option if tapped again
                              setChosenAnswer(null);
                              if (isCorrect) setRunningScore(p => Math.max(0, p - 1));
                            } else if (!chosenAnswer) {
                              setChosenAnswer(option);
                              if (isCorrect) setRunningScore(p => p + 1);
                            }
                          }}
                        >
                          <Text style={[styles.optionText, { color: theme.title, fontWeight: isSelected ? '700' : '400' }]}>{option}</Text>
                          {chosenAnswer && isCorrect && <FontAwesome5 name="check-circle" size={14} color="#4ade80" />}
                          {chosenAnswer && isSelected && !isCorrect && <FontAwesome5 name="times-circle" size={14} color="#f87171" />}
                        </Pressable>
                      );
                    })}

                    {/* RENDER TRUE/FALSE MULTI-STATEMENT INTERFACE */}
                    {isTFStyle && item.statements?.map((statement, sIdx) => {
                      const currentSelection = tfSelections[sIdx];
                      const correctBool = item.answers?.[sIdx];
                      let statementBorder = theme.border;
                      let statementBg = 'transparent';

                      if (tfChecked) {
                        statementBg = currentSelection === correctBool ? 'rgba(74, 222, 128, 0.08)' : 'rgba(248, 113, 113, 0.08)';
                        statementBorder = currentSelection === correctBool ? '#4ade80' : '#f87171';
                      }

                      return (
                        <View key={sIdx} style={[styles.tfStatementRow, { borderColor: statementBorder, backgroundColor: statementBg }]}>
                          <View style={{ flex: 1, paddingRight: 4 }}>
                            <Text style={[styles.tfStatementText, { color: theme.title }]}>{sIdx + 1}. {statement}</Text>
                            {tfChecked && (
                              <Text style={[styles.tfFeedbackText, { color: currentSelection === correctBool ? '#4ade80' : '#f87171' }]}>
                                {`(Selected: ${currentSelection === null ? 'None' : (currentSelection ? 'T' : 'F')} | Correct: ${correctBool ? 'T' : 'F'})`}
                              </Text>
                            )}
                          </View>
                          <View style={styles.tfActionToggleGroup}>
                            <Pressable 
                              disabled={tfChecked}
                              style={[styles.tfToggleBtn, currentSelection === true && { backgroundColor: theme.accent }]}
                              onPress={() => {
                                setTfSelections(p => ({ 
                                  ...p, 
                                  [sIdx]: currentSelection === true ? null : true 
                                }));
                              }}
                            >
                              <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === true ? '#fff' : theme.title }}>T</Text>
                            </Pressable>
                            <Pressable 
                              disabled={tfChecked}
                              style={[styles.tfToggleBtn, currentSelection === false && { backgroundColor: theme.delete }]}
                              onPress={() => {
                                setTfSelections(p => ({ 
                                  ...p, 
                                  [sIdx]: currentSelection === false ? null : false 
                                }));
                              }}
                            >
                              <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === false ? '#fff' : theme.title }}>F</Text>
                            </Pressable>
                          </View>
                        </View>
                      );
                    })}

                    {/* INTERMEDIATE MARKS READOUT PANEL FOR T/F BLOCKS */}
                    {isTFStyle && tfChecked && (
                      <View style={[styles.tfQuestionScoreBadge, { backgroundColor: theme.background }]}>
                        <Text style={[styles.tfQuestionScoreText, { color: theme.title }]}>
                          Question Marks: <Text style={{ color: theme.accent, fontWeight: '800' }}>+{tfQuestionScore} / 5</Text>
                        </Text>
                      </View>
                    )}

                    {/* ACTION NAVIGATORS */}
                    {isTFStyle && !tfChecked && (
                      <Pressable style={[styles.submitActionBtn, { backgroundColor: theme.accent }]} onPress={evaluateTfQuestion}>
                        <Text style={styles.submitActionBtnText}>Check Statements</Text>
                      </Pressable>
                    )}

                    {((!isTFStyle && chosenAnswer) || (isTFStyle && tfChecked)) && (
                      <Pressable style={[styles.submitActionBtn, { backgroundColor: '#4ade80' }]} onPress={handleNextQuestion}>
                        <Text style={styles.submitActionBtnText}>
                          {currentQuestionIdx + 1 === activeDeck.length ? "View Final Results" : "Next Question"}
                        </Text>
                        <FontAwesome5 name="arrow-right" size={12} color="white" style={{ marginLeft: 8 }} />
                      </Pressable>
                    )}
                  </View>
                );
              })()}
            </View>
          )}
        </ScrollView>
      )}

      {/* REUSABLE SELECTOR MODALS */}
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

      <Modal transparent visible={deleteModalVisible} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.customModalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.customModalTitle, { color: theme.title }]}>Delete Dataset</Text>
            <Text style={[styles.customModalSub, { color: theme.subtext }]}>Are you sure you want to permanently delete this file?</Text>
            <View style={styles.customModalActions}>
              <Pressable style={styles.customModalBtn} onPress={() => setDeleteModalVisible(false)}>
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable style={[styles.customModalBtn, { backgroundColor: theme.delete }]} onPress={handleDeleteFile}>
                <Text style={{ color: 'white', fontWeight: '600' }}>Delete</Text>
              </Pressable>
            </View>
          </View>
        </View>
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
  lessonRowContainer: { flexDirection: 'row', alignItems: 'center', borderRadius: 20, borderWidth: 1, marginBottom: 10, paddingRight: 10 },
  lessonPressable: { flex: 1, flexDirection: 'row', alignItems: 'center', padding: 18 },
  lessonTitle: { fontSize: 16, fontWeight: '700', marginBottom: 2 },
  deleteBtn: { width: 38, height: 38, borderRadius: 19, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.02)' },
  quizHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 },
  exitRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  exitText: { fontSize: 13, fontWeight: '700' },
  scoreBadge: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20 },
  scoreBadgeText: { color: 'white', fontSize: 12, fontWeight: '700' },
  progressText: { fontSize: 12, fontWeight: '600', color: 'gray', marginBottom: 12 },
  quizCard: { padding: 20, borderRadius: 22, borderWidth: 1, marginBottom: 16 },
  quizQuestion: { fontSize: 16, fontWeight: '700', marginBottom: 18, lineHeight: 22 },
  optionButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
  optionText: { fontSize: 14, flex: 1, paddingRight: 10 },
  tfStatementRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 12, borderRadius: 14, borderWidth: 1, marginBottom: 10, gap: 10 },
  tfStatementText: { fontSize: 13, lineHeight: 18 },
  tfFeedbackText: { fontSize: 11, fontWeight: '600', marginTop: 3 },
  tfActionToggleGroup: { flexDirection: 'row', gap: 6 },
  tfToggleBtn: { width: 28, height: 28, borderRadius: 8, backgroundColor: 'rgba(0,0,0,0.04)', justifyContent: 'center', alignItems: 'center' },
  tfQuestionScoreBadge: { width: '100%', padding: 12, borderRadius: 12, alignItems: 'center', marginTop: 6, marginBottom: 2 },
  tfQuestionScoreText: { fontSize: 13, fontWeight: '600' },
  submitActionBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 14 },
  submitActionBtnText: { color: 'white', fontWeight: '700', fontSize: 14 },
  resultsCard: { padding: 30, borderRadius: 28, borderWidth: 1, alignItems: 'center', marginTop: 10 },
  iconCircle: { width: 72, height: 72, borderRadius: 36, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
  resultsTitle: { fontSize: 22, fontWeight: '800', marginBottom: 6 },
  resultsSubtitle: { fontSize: 13, textAlign: 'center', lineHeight: 20, paddingHorizontal: 10, marginBottom: 24 },
  finalScoreBox: { width: '100%', padding: 20, borderRadius: 20, alignItems: 'center', marginBottom: 24 },
  finalScoreLabel: { fontSize: 11, fontWeight: '700', letterSpacing: 1, marginBottom: 4 },
  finalScoreText: { fontSize: 32, fontWeight: '900' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', borderRadius: 24, padding: 20, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 15 },
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' },
  customModalContent: { width: '85%', padding: 25, borderRadius: 30, alignItems: 'center' },
  customModalTitle: { fontSize: 20, fontWeight: '700', marginBottom: 10 },
  customModalSub: { textAlign: 'center', marginBottom: 25, lineHeight: 20 },
  customModalActions: { flexDirection: 'row', gap: 15 },
  customModalBtn: { flex: 1, padding: 15, borderRadius: 15, alignItems: 'center' }
});