import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';
import { UnifiedQuizDeck } from './UnifiedQuizDeck';

interface TargetFile {
  filename: string;
  subject: string;
  term: string;
  lesson: string;
}

export default function QuestionSession() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [filesMeta, setFilesMeta] = useState<TargetFile[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  
  const [activeDeck, setActiveDeck] = useState<{ mcqs: any[], shortAnswers: any[], essays: any[] } | null>(null);
  const [mcqAnswers, setMcqAnswers] = useState<{ [key: string]: string }>({});
  const [revealedShort, setRevealedShort] = useState<{ [key: string]: boolean }>({});
  const [revealedEssays, setRevealedEssays] = useState<{ [key: string]: boolean }>({});

  // Selection Dropdowns state
  const [subModalVisible, setSubModalVisible] = useState(false);
  const [termModalVisible, setTermModalVisible] = useState(false);

  const indexLocalFiles = async () => {
    try {
      const folder = `${FileSystem.documentDirectory}questions/`;
      const check = await FileSystem.getInfoAsync(folder);
      if (!check.exists) return;

      const items = await FileSystem.readDirectoryAsync(folder);
      const builds: TargetFile[] = [];

      for (const item of items) {
        if (item.endsWith('.json')) {
          const inner = await FileSystem.readAsStringAsync(`${folder}${item}`);
          const parsed = JSON.parse(inner);
          builds.push({
            filename: item,
            subject: parsed.subject,
            term: parsed.term,
            lesson: parsed.lesson
          });
        }
      }
      setFilesMeta(builds);
    } catch (e) { console.error(e); }
  };

  useFocusEffect(useCallback(() => { indexLocalFiles(); }, []));

  const uniqueSubjects = Array.from(new Set(filesMeta.map(f => f.subject)));
  const uniqueTerms = Array.from(new Set(filesMeta.filter(f => f.subject === selectedSubject).map(f => f.term)));
  const availableLessons = filesMeta.filter(f => f.subject === selectedSubject && f.term === selectedTerm);

  const launchDeck = async (filename: string) => {
    try {
      const targetStr = await FileSystem.readAsStringAsync(`${FileSystem.documentDirectory}questions/${filename}`);
      const parsed = JSON.parse(targetStr);
      
      setActiveDeck({
        mcqs: parsed.multiple_choice_questions || [],
        shortAnswers: parsed.short_answer_questions || [],
        essays: parsed.structured_essay_questions || []
      });
      
      setMcqAnswers({});
      setRevealedShort({});
      setRevealedEssays({});
    } catch (e) { console.error(e); }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Text style={[styles.title, { color: theme.title }]}>Curriculum Review</Text>
      </View>

      {/* SINGLE LINE DROPDOWN HEADERS */}
      {!activeDeck && (
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
            theme-disabled={!selectedSubject}
            style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border, opacity: selectedSubject ? 1 : 0.5 }]}
          >
            <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>
              {selectedTerm ?? "Select Term"}
            </Text>
            <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
          </Pressable>
        </View>
      )}

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

            <UnifiedQuizDeck 
              questions={activeDeck}
              state={{ mcqAnswers, revealedShort, revealedEssays }}
              onMcqSelect={(qIdx, choice) => setMcqAnswers(p => ({ ...p, [qIdx]: choice }))}
              onToggleShort={(qIdx) => setRevealedShort(p => ({ ...p, [qIdx]: !p[qIdx] }))}
              onToggleEssay={(idKey) => setRevealedEssays(p => ({ ...p, [idKey]: !p[idKey] }))}
              theme={theme}
            />
          </View>
        )}
      </ScrollView>

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
  lessonRow: { flexDirection: 'row', alignItems: 'center', padding: 18, borderRadius: 20, borderWidth: 1, marginBottom: 10 },
  lessonTitle: { fontSize: 16, fontWeight: '700', marginBottom: 2 },
  exitRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 20 },
  exitText: { fontSize: 14, fontWeight: '700' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', borderRadius: 24, padding: 20, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 15 },
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' }
});