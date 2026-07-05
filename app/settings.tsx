import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

interface FileItem {
  filename: string;
  subject: string;
  term: string;
  lesson: string;
  preview: string; // Will store the first line
}

export default function Settings() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [files, setFiles] = useState<FileItem[]>([]);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [targetFilename, setTargetFilename] = useState<string | null>(null);

  const loadSavedDecks = async () => {
    try {
      const folderPath = `${FileSystem.documentDirectory}questions/`;
      const folderInfo = await FileSystem.getInfoAsync(folderPath);
      
      if (!folderInfo.exists) {
        setFiles([]);
        return;
      }

      const fileList = await FileSystem.readDirectoryAsync(folderPath);
      const parsedFiles: FileItem[] = [];

      for (const filename of fileList) {
        if (filename.endsWith('.txt')) {
          const content = await FileSystem.readAsStringAsync(`${folderPath}${filename}`);
          
          const cleanName = filename.replace('.txt', '');
          const parts = cleanName.split('-');
          
          const lesson = parts[0] ? parts[0].replace(/_/g, ' ') : 'Unknown Lesson';
          const subject = parts[1] ? parts[1].replace(/_/g, ' ') : 'Unknown Subject';
          const term = parts[2] ? parts[2].replace(/_/g, ' ') : 'Unknown Term';

          // Grab only the first line of text
          const firstLine = content.split('\n')[0] || '';

          parsedFiles.push({
            filename,
            subject,
            term,
            lesson,
            preview: firstLine.trim(),
          });
        }
      }
      setFiles(parsedFiles);
    } catch (e) {
      console.error(e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadSavedDecks();
    }, [])
  );

  const handleDeleteFile = async () => {
    if (!targetFilename) return;

    try {
      const fileUri = `${FileSystem.documentDirectory}questions/${targetFilename}`;
      await FileSystem.deleteAsync(fileUri, { idempotent: true });
      setDeleteModalVisible(false);
      setTargetFilename(null);
      loadSavedDecks();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Pressable style={[styles.backBtn, { backgroundColor: theme.card }]} onPress={() => router.replace("/")}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Storage Settings</Text>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 50 }}>
        <Text style={[styles.sectionTitle, { color: theme.accent }]}>Local Question Decks ({files.length})</Text>

        {files.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="folder-open" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No question files found.</Text>
          </View>
        ) : (
          files.map((file) => (
            <View key={file.filename} style={[styles.fileCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
              
              {/* Card Header Section: Title and Delete Button */}
              <View style={styles.cardHeader}>
                <Text style={[styles.lessonText, { color: theme.title }]} numberOfLines={1}>
                  {file.lesson}
                </Text>
                <Pressable
                  style={styles.deleteBtn}
                  onPress={() => {
                    setTargetFilename(file.filename);
                    setDeleteModalVisible(true);
                  }}
                >
                  <FontAwesome5 name="trash" size={15} color={theme.delete} />
                </Pressable>
              </View>

              {/* Card Body Section: Preview line and Metadata tags */}
              <View style={styles.cardBody}>
                {file.preview ? (
                  <Text style={[styles.previewText, { color: theme.subtext }]} numberOfLines={1}>
                    {file.preview}
                  </Text>
                ) : null}

                <Text style={[styles.subtext, { color: theme.subtext }]}>
                  {file.subject} • {file.term}
                </Text>
                <Text style={[styles.filenameLabel, { color: theme.subtext }]}>{file.filename}</Text>
              </View>

            </View>
          ))
        )}
      </ScrollView>

      {/* DELETE CONFIRMATION MODAL */}
      <Modal transparent visible={deleteModalVisible} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Dataset</Text>
            <Text style={[styles.modalSub, { color: theme.subtext }]}>
              Are you sure you want to permanently delete this file? This action cannot be undone.
            </Text>
            <View style={styles.modalActions}>
              <Pressable style={styles.modalBtn} onPress={() => setDeleteModalVisible(false)}>
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable style={[styles.modalBtn, { backgroundColor: theme.delete }]} onPress={handleDeleteFile}>
                <Text style={{ color: 'white', fontWeight: '600' }}>Delete</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 25, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  scroll: { flex: 1, paddingHorizontal: 20 },
  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 15, marginLeft: 5 },
  fileCard: { padding: 18, borderRadius: 20, borderWidth: 1, marginBottom: 12 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 },
  lessonText: { fontSize: 16, fontWeight: '700', flex: 1, marginRight: 10 },
  deleteBtn: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center' },
  cardBody: { gap: 4 },
  previewText: { fontSize: 13, fontStyle: 'italic', opacity: 0.8, marginBottom: 4 },
  subtext: { fontSize: 12, fontWeight: '600' },
  filenameLabel: { fontSize: 11, fontFamily: 'monospace', opacity: 0.5 },
  emptyContainer: { alignItems: 'center', marginTop: 100 },
  emptyText: { marginTop: 15, fontSize: 16, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '85%', padding: 25, borderRadius: 30, alignItems: 'center' },
  modalTitle: { fontSize: 20, fontWeight: '700', marginBottom: 10 },
  modalSub: { textAlign: 'center', marginBottom: 25, lineHeight: 20 },
  modalActions: { flexDirection: 'row', gap: 15 },
  modalBtn: { flex: 1, padding: 15, borderRadius: 15, alignItems: 'center' },
});