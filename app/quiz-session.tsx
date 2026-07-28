import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';
import * as Sharing from 'expo-sharing';
import React, { useCallback, useEffect, useState } from 'react';
import { StyleSheet, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import DeleteModal from './components/delete-model';
import Footer from './components/footer';
import Header from './components/header';
import ModuleSelector from './components/module-selector';
import { Colors } from './constants/theme';

interface TargetFile {
  filename: string;
  subject: string;
  term: string;
  lesson: string;
}

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const CACHE_DIR = `${FileSystem.documentDirectory}cached-questions/`;

export default function QuestionSession() {
  const router = useRouter();
  const params = useLocalSearchParams<{ launchFilename?: string }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [filesMeta, setFilesMeta] = useState<TargetFile[]>([]);
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
      setFilesMeta(builds.reverse());
    } catch (e) {
      console.error(e);
    }
  };

  useFocusEffect(useCallback(() => { indexLocalFiles(); }, []));

  useEffect(() => {
    if (params.launchFilename) {
      const match = filesMeta.find(f => f.filename === params.launchFilename);
      openLessonReader(match || {
        filename: params.launchFilename,
        lesson: params.launchFilename.replace('.txt', ''),
        subject: '',
        term: ''
      });
    }
  }, [params.launchFilename, filesMeta]);

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
    } catch (e) {
      console.error(e);
    }
  };

 const openLessonReader = (file: TargetFile, initialTab: 'quiz' | 'notes' = 'notes') => {
  router.push({
    pathname: '/read-view',
    params: { 
      filename: file.filename, 
      lesson: file.lesson,
      initialTab 
    }
  });
};

  const copyToClipboard = async (filename: string) => {
    const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
    await Clipboard.setStringAsync(targetStr);
  };

  const shareAsMarkdown = async (filename: string) => {
    const isAvailable = await Sharing.isAvailableAsync();
    if (!isAvailable) {
      alert("Sharing isn't available on this device");
      return;
    }
    const targetStr = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${filename}`);
    const baseName = filename.substring(0, filename.lastIndexOf('.')) || filename;
    const tempMdPath = `${FileSystem.cacheDirectory}${baseName}.md`;

    await FileSystem.writeAsStringAsync(tempMdPath, targetStr, {
      encoding: FileSystem.EncodingType.UTF8,
    });
    await Sharing.shareAsync(tempMdPath, {
      mimeType: 'text/markdown',
      dialogTitle: `Share ${baseName}.md`,
      UTI: 'net.daringfireball.markdown', // Helps iOS recognize the file type
    });
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <Header title="Lectures" onRightButtonPress={() => router.push('/add-questions')} />

     <ModuleSelector
      availableLessons={filesMeta}
      launchDeck={(filename) => {
        const file = filesMeta.find(f => f.filename === filename);
        openLessonReader(
          file || { filename, lesson: filename.replace('.txt', ''), subject: '', term: '' }, 
          'quiz'
        );
      }}
      shareAsMarkdown={shareAsMarkdown}
      copyToClipboard={copyToClipboard}
      onSelectDeleteTarget={(filename) => { setTargetFilename(filename); setDeleteModalVisible(true); }}
      onSelectLesson={openLessonReader} // Automatically defaults to 'notes'
    />

      <DeleteModal visible={deleteModalVisible} onCancel={() => setDeleteModalVisible(false)} onConfirm={handleDeleteFile} />
      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
});