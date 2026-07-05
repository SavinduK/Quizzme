import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Modal, Pressable, ScrollView, StatusBar, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';

const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;

export default function HomeFeed() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [subjects, setSubjects] = useState<string[]>([]);
  const [terms, setTerms] = useState<string[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);

  const [compiledPool, setCompiledPool] = useState<any[]>([]);
  const [mcqAnswers, setMcqAnswers] = useState<{ [key: number]: string }>({});

  const [subPickerVisible, setSubPickerVisible] = useState(false);
  const [termPickerVisible, setTermPickerVisible] = useState(false);

  const fetchAvailableDecks = async () => {
    try {
      const folderInfo = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!folderInfo.exists) return;

      const files = await FileSystem.readDirectoryAsync(CACHE_DIR);
      const uniqueSubjects = new Set<string>();
      const uniqueTerms = new Set<string>();

      for (const file of files) {
        if (file.endsWith('.json')) {
          const cleanName = file.replace('.json', '');
          const parts = cleanName.split('-');
          
          if (parts.length >= 3) {
            const parsedSubject = parts[1].replace(/_/g, ' ');
            const parsedTerm = parts[2].replace(/_/g, ' ');
            uniqueSubjects.add(parsedSubject);
            uniqueTerms.add(parsedTerm);
          }
        }
      }
      setSubjects(Array.from(uniqueSubjects));
      setTerms(Array.from(uniqueTerms));
    } catch (e) { console.error(e); }
  };

  const generateRandomSession = async () => {
    try {
      const folderInfo = await FileSystem.getInfoAsync(CACHE_DIR);
      if (!folderInfo.exists) return;

      const files = await FileSystem.readDirectoryAsync(CACHE_DIR);
      let rawMcqs: any[] = [];

      for (const file of files) {
        if (!file.endsWith('.json')) continue;
        
        const cleanName = file.replace('.json', '');
        const parts = cleanName.split('-');
        
        if (parts.length >= 3) {
          const fileSubject = parts[1].replace(/_/g, ' ');
          const fileTerm = parts[2].replace(/_/g, ' ');

          const matchSubject = !selectedSubject || fileSubject === selectedSubject;
          const matchTerm = !selectedTerm || fileTerm === selectedTerm;

          if (matchSubject && matchTerm) {
            const rawContent = await FileSystem.readAsStringAsync(`${CACHE_DIR}${file}`);
            const parsed = JSON.parse(rawContent);
            
            if (Array.isArray(parsed)) {
              rawMcqs.push(...parsed);
            } else if (parsed && Array.isArray(parsed.questions)) {
              rawMcqs.push(...parsed.questions);
            }
          }
        }
      }

      setCompiledPool(rawMcqs.sort(() => 0.5 - Math.random()).slice(0, 5));
      setMcqAnswers({});
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
        {compiledPool.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="graduation-cap" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No datasets matched selection.</Text>
          </View>
        ) : (
          <View>
            {/* INLINE QUIZ QUESTIONS */}
            {compiledPool.map((item, qIdx) => {
              const chosen = mcqAnswers[qIdx];
              return (
                <View key={qIdx} style={[styles.quizCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
                  <Text style={[styles.quizQuestion, { color: theme.title }]}>{qIdx + 1}. {item.question}</Text>
                  
                  {item.options.map((option: string, oIdx: number) => {
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
                        onPress={() => setMcqAnswers(p => ({ ...p, [qIdx]: option }))}
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
            
            {/* REFRESH BUTTON */}
            <Pressable 
              style={[styles.refreshButton, { backgroundColor: theme.card, borderColor: theme.accent }]}
              onPress={generateRandomSession}
            >
              <FontAwesome5 name="sync-alt" size={14} color={theme.accent} style={{ marginRight: 8 }} />
              <Text style={[styles.refreshButtonText, { color: theme.accent }]}>Refresh Questions</Text>
            </Pressable>
          </View>
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
  modalItem: { width: '100%', paddingVertical: 14, alignItems: 'center', borderBottomWidth: 1, borderBottomColor: 'rgba(0,0,0,0.05)' },
  quizCard: { padding: 20, borderRadius: 22, borderWidth: 1, marginBottom: 16 },
  quizQuestion: { fontSize: 16, fontWeight: '700', marginBottom: 15, lineHeight: 22 },
  optionButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
  optionText: { fontSize: 14, flex: 1, paddingRight: 10 },
  refreshButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', height: 48, borderRadius: 14, borderWidth: 1, marginTop: 10, marginBottom: 30, width: '100%' },
  refreshButtonText: { fontSize: 15, fontWeight: '700' }
});