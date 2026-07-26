import { FontAwesome5 } from '@expo/vector-icons';
import React, { useMemo, useState } from 'react';
import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  useColorScheme,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors, SUBJECT_PALETTES } from '../constants/theme';
import { useModuleFilters } from '../context/filter-context'; // Import context hook

const getSubjectPalette = (subject: string) => {
  if (!subject) return SUBJECT_PALETTES[0];
  let hash = 0;
  const cleanSubject = subject.toLowerCase().trim();
  for (let i = 0; i < cleanSubject.length; i++) {
    hash = cleanSubject.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % SUBJECT_PALETTES.length;
  return SUBJECT_PALETTES[index];
};

interface Lesson {
  filename: string;
  lesson: string;
  subject: string;
  term: string;
}

interface MenuTarget {
  lesson: Lesson;
  position: { top: number; right: number };
}

interface ModuleSelectorProps {
  availableLessons: Lesson[];
  launchDeck: (filename: string) => void;
  copyToClipboard: (filename: string) => void;
  shareAsMarkdown: (filename: string) => void;
  onSelectDeleteTarget: (filename: string) => void;
  onSelectLesson: (file: Lesson) => void;
}

export default function ModuleSelector({
  availableLessons,
  launchDeck,
  copyToClipboard,
  shareAsMarkdown,
  onSelectDeleteTarget,
  onSelectLesson,
}: ModuleSelectorProps) {
  const theme = Colors[useColorScheme() ?? 'light'];

  // --- PERSISTENT FILTERS FROM CONTEXT ---
  const {
    selectedSubject,
    setSelectedSubject,
    selectedTerm,
    setSelectedTerm,
    resetFilters,
  } = useModuleFilters();

  // --- LOCAL UI STATE ONLY ---
  const [searchQuery, setSearchQuery] = useState('');
  const [filterModalVisible, setFilterModalVisible] = useState(false);
  const [activeMenu, setActiveMenu] = useState<MenuTarget | null>(null);

  // --- DYNAMIC DATA PARSING FOR FILTERS ---
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

  const handleOpenMenu = (les: Lesson, event: any) => {
    event.target.measureInWindow((x: number, y: number, width: number, height: number) => {
      setActiveMenu({
        lesson: les,
        position: {
          top: y + height + 4,
          right: 20,
        },
      });
    });
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
          displayedLessons.map((les) => {
            const palette = getSubjectPalette(les.subject);
            return (
              <View key={les.filename} style={[styles.lessonRowContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
                <View style={[styles.cardAccentBar, { backgroundColor: palette.bg }]} />
                
                <Pressable style={styles.lessonPressable} onPress={() => onSelectLesson(les)}>
                  <View style={{ flex: 1, paddingRight: 8 }}>
                    <Text style={[styles.lessonTitle, { color: theme.title }]}>{les.lesson}</Text>
                    <View style={styles.metaRow}>
                      <Text style={[styles.subjectText, { color: theme.subtext }]}>
                        {les.subject}
                      </Text>
                      <Text style={{ color: theme.subtext, fontSize: 12 }}>|</Text>
                      <Text style={{ color: theme.subtext, fontSize: 12 }}>
                        Term {les.term}
                      </Text>
                    </View>
                  </View>
                </Pressable>

                <View style={styles.actionButtonsGroup}>
                  <Pressable 
                    style={styles.iconIconButton} 
                    onPress={() => launchDeck(les.filename)}
                  >
                    <FontAwesome5 name="bolt" size={16} color={theme.accent} />
                  </Pressable>

                  <Pressable 
                    style={styles.iconIconButton} 
                    onPress={(e) => handleOpenMenu(les, e)}
                  >
                    <FontAwesome5 name="ellipsis-v" size={15} color={theme.title} />
                  </Pressable>
                </View>
              </View>
            );
          })
        )}
      </ScrollView>

      {/* --- COMPACT ANCHORED DROPDOWN POPOVER --- */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={activeMenu !== null}
        onRequestClose={() => setActiveMenu(null)}
      >
        <Pressable style={styles.popoverOverlay} onPress={() => setActiveMenu(null)}>
          {activeMenu && (
            <View
              style={[
                styles.compactDropdownCard,
                {
                  backgroundColor: theme.card,
                  borderColor: theme.border,
                  top: activeMenu.position.top,
                  right: activeMenu.position.right,
                },
              ]}
            >
              <Pressable
                style={styles.compactMenuItem}
                onPress={() => {
                  const target = activeMenu.lesson;
                  setActiveMenu(null);
                  shareAsMarkdown(target.filename);
                }}
              >
                <FontAwesome5 name="share-alt" size={14} color={theme.accent} style={styles.compactIcon} />
                <Text style={[styles.compactMenuText, { color: theme.title }]}>Share</Text>
              </Pressable>
              
              <Pressable
                style={styles.compactMenuItem}
                onPress={() => {
                  const target = activeMenu.lesson;
                  setActiveMenu(null);
                  copyToClipboard(target.filename);
                }}
              >
                <FontAwesome5 name="copy" size={14} color="#007AFF" style={styles.compactIcon} />
                <Text style={[styles.compactMenuText, { color: theme.title }]}>Copy</Text>
              </Pressable>

              <Pressable
                style={[styles.compactMenuItem, { borderBottomWidth: 0 }]}
                onPress={() => {
                  const target = activeMenu.lesson;
                  setActiveMenu(null);
                  onSelectDeleteTarget(target.filename);
                }}
              >
                <FontAwesome5 name="trash" size={14} color={theme.delete} style={styles.compactIcon} />
                <Text style={[styles.compactMenuText, { color: theme.delete }]}>Delete</Text>
              </Pressable>
            </View>
          )}
        </Pressable>
      </Modal>

      {/* --- INTEGRATED FILTER PANEL MODAL --- */}
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
              <View style={[styles.sheetHeader, { borderBottomColor: theme.border }]}>
                <Text style={[styles.sheetTitle, { color: theme.title }]}>Filter Options</Text>
                <Pressable onPress={() => setFilterModalVisible(false)} style={styles.sheetCloseBtn}>
                  <FontAwesome5 name="times" size={16} color={theme.title} />
                </Pressable>
              </View>

              <ScrollView style={styles.sheetContentContainer}>
                <Text style={[styles.sheetGroupLabel, { color: theme.subtext }]}>Subject</Text>
                <View style={styles.chipWrapperRow}>
                  <Pressable
                    onPress={() => setSelectedSubject(null)}
                    style={[styles.chip, !selectedSubject ? { backgroundColor: theme.buttons, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.chipText, !selectedSubject ? { color: theme.accent } : { color: theme.title }]}>All</Text>
                  </Pressable>
                  {uniqueSubjects.map((sub) => {
                    const isSelected = selectedSubject?.toLowerCase() === sub.toLowerCase();
                    return (
                      <Pressable
                        key={sub}
                        onPress={() => setSelectedSubject(sub)}
                        style={[styles.chip, isSelected ? { backgroundColor: theme.buttons, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.chipText, isSelected ? { color: theme.accent } : { color: theme.title }]}>{sub}</Text>
                      </Pressable>
                    );
                  })}
                </View>

                <Text style={[styles.sheetGroupLabel, { color: theme.subtext, marginTop: 24 }]}>Term</Text>
                <View style={styles.chipWrapperRow}>
                  <Pressable
                    onPress={() => setSelectedTerm(null)}
                    style={[styles.chip, !selectedTerm ? { backgroundColor: theme.buttons, borderColor: theme.accent } : { borderColor: theme.border }]}
                  >
                    <Text style={[styles.chipText, !selectedTerm ? { color: theme.accent } : { color: theme.title }]}>All</Text>
                  </Pressable>
                  {uniqueTerms.map((trm) => {
                    const isSelected = selectedTerm?.toLowerCase() === trm.toLowerCase();
                    return (
                      <Pressable
                        key={trm}
                        onPress={() => setSelectedTerm(trm)}
                        style={[styles.chip, isSelected ? { backgroundColor: theme.buttons, borderColor: theme.accent } : { borderColor: theme.border }]}
                      >
                        <Text style={[styles.chipText, isSelected ? { color: theme.accent } : { color: theme.title }]}>{trm}</Text>
                      </Pressable>
                    );
                  })}
                </View>

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
  container: { flex: 1, paddingVertical: 10 },
  searchBarRow: { flexDirection: 'row', paddingHorizontal: 20, paddingVertical: 10, gap: 10 },
  searchContainer: { flex: 1, height: 48, borderRadius: 14, borderWidth: 1, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14 },
  searchIcon: { marginRight: 10 },
  searchInput: { flex: 1, fontSize: 15, height: '100%', paddingVertical: 0 },
  clearBtn: { padding: 4 },
  filterBtn: { width: 48, height: 48, borderRadius: 14, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  feedBody: { flexGrow: 1, paddingHorizontal: 20, paddingTop: 8, paddingBottom: 30 },
  lessonRowContainer: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    borderRadius: 16, 
    borderWidth: 1, 
    marginBottom: 16, 
    paddingRight: 12,
    overflow: 'hidden'
  },
  cardAccentBar: {
    width: 6,
    alignSelf: 'stretch',
  },
  lessonPressable: { flex: 1, paddingVertical: 16, paddingLeft: 12, paddingRight: 8 },
  lessonTitle: { fontSize: 15, fontWeight: '700', marginBottom: 4, lineHeight: 20, textTransform: 'uppercase' },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 4 },
  subjectText: { fontSize: 12, textTransform: 'capitalize', fontWeight: '500' },
  actionButtonsGroup: { flexDirection: 'row', alignItems: 'center', gap: 2 },
  iconIconButton: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  emptyContainer: { alignItems: 'center', marginTop: 50, paddingHorizontal: 40 },
  popoverOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.15)',
  },
  compactDropdownCard: {
    position: 'absolute',
    minWidth: 130,
    borderRadius: 12,
    borderWidth: 1,
    paddingVertical: 4,
    paddingHorizontal: 4,
    elevation: 6,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.18,
    shadowRadius: 6,
  },
  compactMenuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 8,
  },
  compactIcon: {
    width: 18,
    textAlign: 'center',
    marginRight: 8,
  },
  compactMenuText: {
    fontSize: 13,
    fontWeight: '600',
  },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalDismissTapZone: { flex: 1 },
  modalSheet: { borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingBottom: 20, maxHeight: '80%' },
  sheetHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 18, borderWidth: 1, borderTopWidth: 0, borderLeftWidth: 0, borderRightWidth: 0 },
  sheetTitle: { fontSize: 18, fontWeight: '700' },
  sheetCloseBtn: { padding: 4 },
  sheetContentContainer: { padding: 24 },
  sheetGroupLabel: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1 },
  chipWrapperRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 12 },
  chip: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: 12, borderWidth: 1 },
  chipText: { fontSize: 14, fontWeight: '600' },
  sheetFooterActions: { flexDirection: 'row', gap: 12, marginTop: 40, marginBottom: 20 },
  actionBtnSecondary: { flex: 1, height: 48, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  actionBtnPrimary: { flex: 2, height: 48, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
});