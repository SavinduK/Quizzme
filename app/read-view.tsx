import { FontAwesome5 } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Modal,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from 'react-native';
import Markdown from 'react-native-markdown-display';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import { Colors } from './constants/theme'; // Adjust relative path to match your layout

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;

export default function LessonReaderScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ filename: string; lesson: string }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [loading, setLoading] = useState(true);
  const [readingContent, setReadingContent] = useState<string>('');

  // Editing state
  const [isEditing, setIsEditing] = useState(false);
  const [editableText, setEditableText] = useState('');
  const [saving, setSaving] = useState(false);
  const [selection, setSelection] = useState<{ start: number; end: number } | undefined>(undefined);

  // Success Modal state
  const [showSuccessModal, setShowSuccessModal] = useState(false);

  useEffect(() => {
    const loadContent = async () => {
      if (!params.filename) {
        Alert.alert('Error', 'No lesson file specified.');
        router.back();
        return;
      }
      try {
        const content = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
        setReadingContent(content);
      } catch (e) {
        console.error(e);
        Alert.alert('Error', 'Could not load lesson contents.');
        router.back();
      } finally {
        setLoading(false);
      }
    };

    loadContent();
  }, [params.filename]);

  const copyToClipboard = async () => {
    if (!params.filename) return;
    try {
      const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
      await Clipboard.setStringAsync(targetStr);
    } catch (e) {
      console.error(e);
    }
  };

  const handleLaunchQuiz = () => {
    router.replace({
      pathname: '/quiz-session',
      params: { launchFilename: params.filename },
    });
  };

  const handleOpenEditModal = () => {
    setEditableText(readingContent);
    // Force the cursor to position 0 (top-left) when opening the editor
    setSelection({ start: 0, end: 0 });
    setIsEditing(true);
  };

  const handleSaveEdit = async () => {
    if (!params.filename) return;
    setSaving(true);
    try {
      const filePath = `${QUESTIONS_DIR}${params.filename}`;
      await FileSystem.writeAsStringAsync(filePath, editableText);
      setReadingContent(editableText);
      setIsEditing(false);
      
      // Show styled success modal
      setShowSuccessModal(true);
    } catch (e) {
      console.error(e);
      Alert.alert('Error', 'Failed to save edits.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: theme.background, justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={theme.accent} />
        <Text style={{ color: theme.subtext, marginTop: 12, fontWeight: '500' }}>Loading Content...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={[styles.readerHeader, { borderBottomColor: theme.border }]}>
        <View style={{ flexDirection: 'row', alignItems: 'center', width: '100%' }}>
          <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
            <FontAwesome5 name="arrow-left" size={16} color={theme.text} />
          </TouchableOpacity>
          <View style={{ flex: 1, marginRight: 10 }}>
            <Text
              style={[styles.lessonTitleLarge, { color: theme.text, marginHorizontal: 10 }]}
              numberOfLines={1}
              ellipsizeMode="tail"
            >
              {params.lesson || 'View Lesson'}
            </Text>
          </View>

          <View style={[styles.readerActions, { flexDirection: 'row', alignItems: 'center' }]}>
            <TouchableOpacity style={[styles.actionChip, { backgroundColor: theme.buttons }]} onPress={handleLaunchQuiz}>
              <FontAwesome5 name="bolt" size={12} color={theme.accent} />
              <Text style={[styles.actionChipText, { color: theme.accent }]}> Quiz</Text>
            </TouchableOpacity>

            <TouchableOpacity style={[styles.actionChip, { backgroundColor: theme.accent + '15' }]} onPress={copyToClipboard}>
              <FontAwesome5 name="copy" size={12} color={theme.accent} />
              <Text style={[styles.actionChipText, { color: theme.accent }]}>Copy</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        <Markdown 
    
          style={{
            body: { color: theme.text, fontSize: 15, lineHeight: 24 },
            heading1: { color: theme.text, fontWeight: '700', marginVertical: 10 },
            heading2: { color: theme.text, fontWeight: '600', marginVertical: 8 },
            paragraph: { marginVertical: 6 },
            link: { color: theme.accent },
            bullet_list: { color: theme.text },
            ordered_list: { color: theme.text },
          }}
        >
          {readingContent}
        </Markdown>
      </ScrollView>

      {/* Floating Action Button (FAB) for Editing */}
      <TouchableOpacity
        style={[styles.fab, { backgroundColor: theme.accent }]}
        onPress={handleOpenEditModal}
        activeOpacity={0.8}
      >
        <FontAwesome5 name="pen" size={18} color="#FFFFFF" />
      </TouchableOpacity>

      {/* Edit Modal */}
      <Modal visible={isEditing} animationType="slide" presentationStyle="pageSheet" onRequestClose={() => setIsEditing(false)}>
        <SafeAreaView style={[styles.modalContainer, { backgroundColor: theme.background }]}>
          <View style={[styles.modalHeader, { borderBottomColor: theme.border }]}>
            <TouchableOpacity onPress={() => setIsEditing(false)}>
              <Text style={[styles.modalCancel, { color: theme.subtext }]}>Cancel</Text>
            </TouchableOpacity>
            <Text style={[styles.modalTitle, { color: theme.text }]}>Edit Lesson</Text>
            <TouchableOpacity onPress={handleSaveEdit} disabled={saving}>
              {saving ? (
                <ActivityIndicator size="small" color={theme.accent} />
              ) : (
                <Text style={[styles.modalSave, { color: theme.accent }]}>Save</Text>
              )}
            </TouchableOpacity>
          </View>

          <TextInput
            style={[styles.editorInput, { color: theme.text }]}
            multiline
            value={editableText}
            onChangeText={(text) => {
              setEditableText(text);
              if (selection) setSelection(undefined);
            }}
            selection={selection}
            onSelectionChange={(e) => setSelection(e.nativeEvent.selection)}
            placeholder="Type lesson markdown content..."
            placeholderTextColor={theme.subtext}
            textAlignVertical="top"
            autoFocus
          />
        </SafeAreaView>
      </Modal>

      {/* Custom Styled Success Modal */}
      <Modal
        visible={showSuccessModal}
        transparent
        animationType="fade"
        onRequestClose={() => setShowSuccessModal(false)}
      >
        <View style={styles.alertBackdrop}>
          <View style={[styles.alertCard, { backgroundColor: theme.background, borderColor: theme.border }]}>
            <View style={[styles.alertIconCircle, { backgroundColor: theme.accent + '20' }]}>
              <FontAwesome5 name="check" size={24} color={theme.accent} />
            </View>
            <Text style={[styles.alertTitle, { color: theme.text }]}>Updated!</Text>
            <Text style={[styles.alertMessage, { color: theme.subtext }]}>
              Lesson content has been saved successfully.
            </Text>
            <TouchableOpacity
              style={[styles.alertButton, { backgroundColor: theme.accent }]}
              onPress={() => setShowSuccessModal(false)}
              activeOpacity={0.8}
            >
              <Text style={styles.alertButtonText}>Done</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  readerHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
    marginBottom: 15,
  },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, marginRight: 20 },
  readerActions: { flexDirection: 'row', gap: 8 },
  actionChip: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 12, gap: 6 },
  actionChipText: { fontSize: 13, fontWeight: '700' },
  lessonTitleLarge: { fontSize: 20, fontWeight: '700', lineHeight: 30, textTransform: 'uppercase' },

  /* Floating Action Button (FAB) */
  fab: {
    position: 'absolute',
    bottom: 90,
    right: 20,
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    zIndex: 99,
  },

  /* Modal Styling */
  modalContainer: { flex: 1 },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  modalTitle: { fontSize: 17, fontWeight: '700' },
  modalCancel: { fontSize: 16 },
  modalSave: { fontSize: 16, fontWeight: '700' },
  editorInput: { flex: 1, padding: 20, fontSize: 15, lineHeight: 22 },

  /* Alert / Confirmation Modal Styling */
  alertBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 30,
  },
  alertCard: {
    width: '100%',
    maxWidth: 320,
    borderRadius: 20,
    padding: 24,
    alignItems: 'center',
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 10,
    elevation: 6,
  },
  alertIconCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  alertTitle: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  alertMessage: {
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 20,
  },
  alertButton: {
    width: '100%',
    paddingVertical: 12,
    borderRadius: 12,
    alignItems: 'center',
  },
  alertButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
});