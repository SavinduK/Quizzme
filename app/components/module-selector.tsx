import { FontAwesome5 } from '@expo/vector-icons';
import React, { useMemo, useState } from 'react';
import {
  Modal,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  useColorScheme,
  View,
} from 'react-native';
import { Colors } from '../constants/theme';

interface Lesson {
  filename: string;
  lesson: string;
  subject: string;
  term: string;
}

interface ModuleSelectorProps {
  availableLessons: Lesson[]; // Raw builds array directly from indexLocalFiles
  launchDeck: (filename: string) => void;
  copyToClipboard: (filename: string) => void;
  onSelectDeleteTarget: (filename: string) => void;
}

export default function ModuleSelector({
  availableLessons,
  launchDeck,
  copyToClipboard,
  onSelectDeleteTarget,
}: ModuleSelectorProps) {
  const theme = Colors[useColorScheme() ?? 'light'];

  // --- INTERNAL COMPONENT STATE ---
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSubject, setSelectedSubject] = useState<string | null>(null);
  const [selectedTerm, setSelectedTerm] = useState<string | null>(null);
  const [filterModalVisible, setFilterModalVisible] = useState(false);

  // --- DYNAMIC DATA PARSING FOR FILTERS ---
  // Automatically gets unique lists of subjects and terms from the raw files array
  const { uniqueSubjects, uniqueTerms } = useMemo(() => {
    const subjects = new Set<string>();
    const terms = new Set<string>();

    availableLessons.forEach((item) => {
      if (item.subject) subjects.add(item.subject);
      if (item.term) terms.add(item.term);
    });

    return {
      uniqueSubjects: Array.from(subjects).sort(),
      uniqueTerms: Array.from(terms).sort(),
    };
  }, [availableLessons]);

  // --- LIVE INLINE FILTER LOOP ---
  const displayedLessons = useMemo(() => {
    return availableLessons.filter((les) => {
      const matchesSearch =
        searchQuery.trim() === '' ||
        les.lesson.toLowerCase().includes(searchQuery.toLowerCase()) ||
        les.subject.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesSubject =
        !selectedSubject || les.subject.toLowerCase() === selectedSubject.toLowerCase();

      const matchesTerm =
        !selectedTerm || les.term.toLowerCase() === selectedTerm.toLowerCase();

      return matchesSearch && matchesSubject && matchesTerm;
    });
  }, [searchQuery, selectedSubject, selectedTerm, availableLessons]);

  const hasFilter = selectedSubject || selectedTerm;
  const filterLabel = [selectedSubject, selectedTerm].filter(Boolean).join(' • ');

  // Clear helper to wipe filters back to default
  const resetFilters = () => {
    setSelectedSubject(null);
    setSelectedTerm(null);
  };

  return (
    <View style={styles.container}>
      {/* Search & Filter Header Control Bar */}
      <View style={styles.searchBarRow}>
        <View style={[styles.searchContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
          <FontAwesome5 name="search" size={14} color={theme.subtext} style={styles.searchIcon} />
          <TextInput
            placeholder="Search modules..."
            placeholderTextColor={theme.subtext}
            value={searchQuery}
            onChangeText={setSearchQuery}
            style={[styles.searchInput, { color: theme.title }]}
          />
          {searchQuery.length > 0 && (
            <Pressable onPress={() => setSearchQuery('')} style={styles.clearBtn}>
              <FontAwesome5 name="times-circle" size={14} color={theme.subtext} />
            </Pressable>
          )}
        </View>

        <Pressable
          onPress={() => setFilterModalVisible(true)}
          style={[
            styles.filterBtn,
            { backgroundColor: theme.card, borderColor: hasFilter ? theme.accent : theme.border },
          ]}
        >
          <FontAwesome5 name="sliders-h" size={16} color={hasFilter ? theme.accent : theme.title} />
        </Pressable>
      </View>

      {/* Primary Module Feed List */}
      <ScrollView contentContainerStyle={styles.feedBody} showsVerticalScrollIndicator={false}>

        {displayedLessons.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="folder-open" size={40} color={theme.border} style={{ marginBottom: 12 }} />
            <Text style={{ color: theme.subtext, textAlign: 'center', fontWeight: '500' }}>
              No matches found for your current criteria.
            </Text>
          </View>
        ) : (
          displayedLessons.map((les) => (
            <View key={les.filename} style={[styles.lessonRowContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <Pressable style={styles.lessonPressable} onPress={() => launchDeck(les.filename)}>
                <View style={{ flex: 1, paddingRight: 8 }}>
                  <Text style={[styles.lessonTitle, { color: theme.title }]}>{les.lesson}</Text>
                  <Text style={{ color: theme.subtext, fontSize: 12,textTransform:'capitalize' }}>
                    {les.subject} | Term {les.term}
                  </Text>
                  
                </View>
              </Pressable>

              <View style={styles.actionButtonsGroup}>
                <Pressable style={styles.iconIconButton} onPress={() => copyToClipboard(les.filename)}>
                  <FontAwesome5 name="copy" size={14} color={theme.subtext} />
                </Pressable>
                <Pressable
                  style={[styles.deleteBtn, { backgroundColor: 'rgba(255,0,0,0.04)' }]}
                  onPress={() => onSelectDeleteTarget(les.filename)}
                >
                  <FontAwesome5 name="trash" size={13} color={theme.delete} />
                </Pressable>
              </View>
            </View>
          ))
        )}
      </ScrollView>

      {/* --- INTEGRATED FILTER PANEL MODAL (BOTTOM PANEL SLIDEOUT) --- */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={filterModalVisible}
        onRequestClose={() => setFilterModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <Pressable style={styles.modalDismissTapZone} onPress={() => setFilterModalVisible(false)} />
          
          <View style={[styles.modalSheet, { backgroundColor: theme.card }]}>
            <SafeAreaView>
              {/* Header */}
              <View style={[styles.sheetHeader, { borderBottomColor: theme.border }]}>
                <Text style={[styles.sheetTitle, { color: theme.title }]}>Filter Options</Text>
                <Pressable onPress={() => setFilterModalVisible(false)} style={styles.sheetCloseBtn}>
                  <FontAwesome5 name="times" size={16} color={theme.title} />
                </Pressable>
              </View>

              <ScrollView style={styles.sheetContentContainer}>
                {/* Subject Block Section */}
                <Text style={[styles.sheetGroupLabel, { color: theme.subtext }]}>Subject</Text>
                <View style={styles.chipWrapperRow}>
                  <Pressable
                    onPress={() => setSelectedSubject(null)}
                    style={[styles.chip, !selectedSubject ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.chipText, !selectedSubject ? { color: '#FFF' } : { color: theme.title }]}>All</Text>
                  </Pressable>
                  {uniqueSubjects.map((sub) => {
                    const isSelected = selectedSubject?.toLowerCase() === sub.toLowerCase();
                    return (
                      <Pressable
                        key={sub}
                        onPress={() => setSelectedSubject(sub)}
                        style={[styles.chip, isSelected ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.chipText, isSelected ? { color: '#FFF' } : { color: theme.title }]}>{sub}</Text>
                      </Pressable>
                    );
                  })}
                </View>

                {/* Term Block Section */}
                <Text style={[styles.sheetGroupLabel, { color: theme.subtext, marginTop: 24 }]}>Term</Text>
                <View style={styles.chipWrapperRow}>
                  <Pressable
                    onPress={() => setSelectedTerm(null)}
                    style={[styles.chip, !selectedTerm ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.chipText, !selectedTerm ? { color: '#FFF' } : { color: theme.title }]}>All</Text>
                  </Pressable>
                  {uniqueTerms.map((trm) => {
                    const isSelected = selectedTerm?.toLowerCase() === trm.toLowerCase();
                    return (
                      <Pressable
                        key={trm}
                        onPress={() => setSelectedTerm(trm)}
                        style={[styles.chip, isSelected ? { backgroundColor: theme.accent, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.chipText, isSelected ? { color: '#FFF' } : { color: theme.title }]}>{trm}</Text>
                      </Pressable>
                    );
                  })}
                </View>

                {/* Bottom Action Footer buttons */}
                <View style={styles.sheetFooterActions}>
                  <Pressable onPress={resetFilters} style={[styles.actionBtnSecondary, { borderColor: theme.border }]}>
                    <Text style={{ color: theme.title, fontWeight: '600' }}>Reset All</Text>
                  </Pressable>
                  <Pressable onPress={() => setFilterModalVisible(false)} style={[styles.actionBtnPrimary, { backgroundColor: theme.accent }]}>
                    <Text style={{ color: '#FFF', fontWeight: '700' }}>Apply Filters</Text>
                  </Pressable>
                </View>
              </ScrollView>
            </SafeAreaView>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  searchBarRow: { flexDirection: 'row', paddingHorizontal: 20, marginBottom: 12, marginTop: 16, gap: 10 },
  searchContainer: { flex: 1, height: 48, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14 },
  searchIcon: { marginRight: 10 },
  searchInput: { flex: 1, fontSize: 15, height: '100%', paddingVertical: 0 },
  clearBtn: { padding: 4 },
  filterBtn: { width: 48, height: 48, borderRadius: 14, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  activeFilterBadgeRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 22, marginBottom: 12 },
  filterIndicatorText: { fontSize: 13, flex: 1 },
  resetBadgeBtn: { marginLeft: 10, paddingVertical: 2, paddingHorizontal: 6 },
  feedBody: { flexGrow:1, paddingHorizontal: 20, paddingTop: 8, paddingBottom: 30 },
  sectionLabel: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1.2, marginBottom: 14 },
  lessonRowContainer: { flexDirection: 'row', alignItems: 'center', borderRadius: 16, borderWidth: 1, marginBottom: 10, paddingRight: 12 },
  lessonPressable: { flex: 1, padding: 16 },
  lessonTitle: { fontSize: 15, fontWeight: '700', marginBottom: 4, lineHeight: 20,textTransform:'capitalize' },
  actionButtonsGroup: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  iconIconButton: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  deleteBtn: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  emptyContainer: { alignItems: 'center', marginTop: 50, paddingHorizontal: 40 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalDismissTapZone: { flex: 1 },
  modalSheet: { borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingBottom: 20, maxHeight: '80%' },
  sheetHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 18, borderWidth: 1, borderTopWidth: 0, borderLeftWidth: 0, borderRightWidth: 0 },
  sheetTitle: { fontSize: 18, fontWeight: '700' },
  sheetCloseBtn: { padding: 4 },
  sheetContentContainer: { padding: 24 },
  sheetGroupLabel: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  chipWrapperRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  chip: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20, borderWidth: 1 },
  chipText: { fontSize: 14, fontWeight: '600' },
  sheetFooterActions: { flexDirection: 'row', gap: 12, marginTop: 40, marginBottom: 20 },
  actionBtnSecondary: { flex: 1, height: 48, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  actionBtnPrimary: { flex: 2, height: 48, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
});