import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Modal, Pressable, ScrollView, StatusBar, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';
import { UnifiedQuizDeck } from './UnifiedQuizDeck';

export default function HomeFeed() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [subjects, setSubjects] = useState<string[]>([]);
  const [terms, setTerms] = useState<string[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);

  const [compiledPool, setCompiledPool] = useState({ mcqs: [], shortAnswers: [], essays: [] });
  const [mcqAnswers, setMcqAnswers] = useState<{ [key: string]: string }>({});
  const [revealedShort, setRevealedShort] = useState<{ [key: string]: boolean }>({});
  const [revealedEssays, setRevealedEssays] = useState<{ [key: string]: boolean }>({});

  // Dropdown UI Visibility States
  const [subPickerVisible, setSubPickerVisible] = useState(false);
  const [termPickerVisible, setTermPickerVisible] = useState(false);

  const fetchAvailableDecks = async () => {
    try {
      const folderPath = `${FileSystem.documentDirectory}questions/`;
      const folderInfo = await FileSystem.getInfoAsync(folderPath);
      if (!folderInfo.exists) return;

      const files = await FileSystem.readDirectoryAsync(folderPath);
      const uniqueSubjects = new Set<string>();
      const uniqueTerms = new Set<string>();

      for (const file of files) {
        if (file.endsWith('.json')) {
          const content = await FileSystem.readAsStringAsync(`${folderPath}${file}`);
          const parsed = JSON.parse(content);
          if (parsed.subject) uniqueSubjects.add(parsed.subject);
          if (parsed.term) uniqueTerms.add(parsed.term);
        }
      }
      setSubjects(Array.from(uniqueSubjects));
      setTerms(Array.from(uniqueTerms));
    } catch (e) { console.error(e); }
  };

  const generateRandomSession = async () => {
    try {
      const folderPath = `${FileSystem.documentDirectory}questions/`;
      const files = await FileSystem.readDirectoryAsync(folderPath);
      
      let rawMcqs: any[] = [];
      let rawShorts: any[] = [];
      let rawEssays: any[] = [];

      for (const file of files) {
        if (!file.endsWith('.json')) continue;
        const rawContent = await FileSystem.readAsStringAsync(`${folderPath}${file}`);
        const parsed = JSON.parse(rawContent);

        const matchSubject = !selectedSubject || parsed.subject === selectedSubject;
        const matchTerm = !selectedTerm || parsed.term === selectedTerm;

        if (matchSubject && matchTerm) {
          if (parsed.multiple_choice_questions) rawMcqs.push(...parsed.multiple_choice_questions);
          if (parsed.short_answer_questions) rawShorts.push(...parsed.short_answer_questions);
          if (parsed.structured_essay_questions) rawEssays.push(...parsed.structured_essay_questions);
        }
      }

      setCompiledPool({
        mcqs: rawMcqs.sort(() => 0.5 - Math.random()).slice(0, 4),
        shortAnswers: rawShorts.sort(() => 0.5 - Math.random()).slice(0, 3),
        essays: rawEssays.sort(() => 0.5 - Math.random()).slice(0, 3)
      });

      setMcqAnswers({});
      setRevealedShort({});
      setRevealedEssays({});
    } catch (e) { console.error(e); }
  };

  useFocusEffect(useCallback(() => { fetchAvailableDecks(); }, []));
  useFocusEffect(useCallback(() => { generateRandomSession(); }, [selectedSubject, selectedTerm]));

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      <View style={styles.header}>
        <View>
          <Text style={[styles.headerSubtitle, { color: theme.subtext }]}>Random Evaluation Mode</Text>
          <Text style={[styles.headerTitle, { color: theme.title }]}>Daily Flash-Quizzes</Text>
        </View>
        <Pressable style={[styles.addBtn, { backgroundColor: theme.accent }]} onPress={() => router.push('/add-questions')}>
          <FontAwesome5 name="plus" size={16} color="white" />
        </Pressable>
      </View>

      {/* SINGLE ROW DROPDOWN PICKERS */}
      <View style={styles.dropdownRow}>
        <Pressable 
          style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} 
          onPress={() => setSubPickerVisible(true)}
        >
          <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>
            {selectedSubject ?? "All Subjects"}
          </Text>
          <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
        </Pressable>

        <Pressable 
          style={[styles.dropdown, { backgroundColor: theme.card, borderColor: theme.border }]} 
          onPress={() => setTermPickerVisible(true)}
        >
          <Text style={[styles.dropdownText, { color: theme.title }]} numberOfLines={1}>
            {selectedTerm ?? "All Terms"}
          </Text>
          <FontAwesome5 name="chevron-down" size={12} color={theme.subtext} />
        </Pressable>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {compiledPool.mcqs.length === 0 && compiledPool.shortAnswers.length === 0 && compiledPool.essays.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="graduation-cap" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No datasets matched selection.</Text>
          </View>
        ) : (
          <UnifiedQuizDeck 
            questions={compiledPool}
            state={{ mcqAnswers, revealedShort, revealedEssays }}
            onMcqSelect={(qIdx, choice) => setMcqAnswers(p => ({ ...p, [qIdx]: choice }))}
            onToggleShort={(qIdx) => setRevealedShort(p => ({ ...p, [qIdx]: !p[qIdx] }))}
            onToggleEssay={(idKey) => setRevealedEssays(p => ({ ...p, [idKey]: !p[idKey] }))}
            theme={theme}
          />
        )}
      </ScrollView>

      {/* SUBJECT SELECTION MODAL */}
      <Modal visible={subPickerVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setSubPickerVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Subject</Text>
            <ScrollView style={{ width: '100%', maxHeight: 300 }}>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedSubject(null); setSubPickerVisible(false); }}>
                <Text style={{ color: theme.accent, fontWeight: '700' }}>All Subjects</Text>
              </Pressable>
              {subjects.map(sub => (
                <Pressable key={sub} style={styles.modalItem} onPress={() => { setSelectedSubject(sub); setSubPickerVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '500' }}>{sub}</Text>
                </Pressable>
              ))}
            </ScrollView>
          </View>
        </Pressable>
      </Modal>

      {/* TERM SELECTION MODAL */}
      <Modal visible={termPickerVisible} transparent animationType="fade">
        <Pressable style={styles.modalOverlay} onPress={() => setTermPickerVisible(false)}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Select Term</Text>
            <ScrollView style={{ width: '100%', maxHeight: 300 }}>
              <Pressable style={styles.modalItem} onPress={() => { setSelectedTerm(null); setTermPickerVisible(false); }}>
                <Text style={{ color: theme.accent, fontWeight: '700' }}>All Terms</Text>
              </Pressable>
              {terms.map(t => (
                <Pressable key={t} style={styles.modalItem} onPress={() => { setSelectedTerm(t); setTermPickerVisible(false); }}>
                  <Text style={{ color: theme.title, fontWeight: '500' }}>{t}</Text>
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
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 25, paddingTop: 20 },
  headerSubtitle: { fontSize: 13, fontWeight: '600', marginBottom: 2 },
  headerTitle: { fontSize: 26, fontWeight: '800', letterSpacing: -0.5 },
  addBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  dropdownRow: { flexDirection: 'row', paddingHorizontal: 25, marginVertical: 15, gap: 12 },
  dropdown: { flex: 1, height: 46, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 14 },
  dropdownText: { fontSize: 14, fontWeight: '600', flex: 1, marginRight: 8 },
  scroll: { flex: 1, paddingHorizontal: 20 },
  emptyContainer: { alignItems: 'center', marginTop: 80 },
  emptyText: { marginTop: 15, fontSize: 15, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', borderRadius: 24, padding: 20, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 15 },
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' }
});