import { FontAwesome5 } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View, useColorScheme } from 'react-native';
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
  const [readingContent, setReadingContent] = useState<string>("");

  useEffect(() => {
    const loadContent = async () => {
      if (!params.filename) {
        Alert.alert("Error", "No lesson file specified.");
        router.back();
        return;
      }
      try {
        const content = await FileSystem.readAsStringAsync(`${QUESTIONS_DIR}${params.filename}`);
        setReadingContent(content);
      } catch (e) {
        console.error(e);
        Alert.alert("Error", "Could not load lesson contents.");
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
      Alert.alert("Copied", "Lesson text copied to clipboard!");
    } catch (e) {
      console.error(e);
    }
  };

  const handleLaunchQuiz = () => {
    // Navigates back or directly pushes parameter to trigger deck configuration 
    router.replace({
      pathname: '/quiz-session', // Maps back to your main panel
      params: { launchFilename: params.filename }
    });
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
        <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.text} />
          <Text style={[styles.lessonTitleLarge, { color: theme.text, marginHorizontal: 10 }]} numberOfLines={1} ellipsizeMode='tail'>
            {params.lesson || "View Lesson"}
          </Text>
        </TouchableOpacity>

        <View style={styles.readerActions}>
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

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        <Markdown 
          style={{
            body: { color: theme.text, fontSize: 15, lineHeight: 24 },
            heading1: { color: theme.text, fontWeight: '700', marginVertical: 10 },
            heading2: { color: theme.text, fontWeight: '600', marginVertical: 8 },
            paragraph: { marginVertical: 6 },
            link: { color: theme.accent },
            bullet_list: { color: theme.text },
            ordered_list: { color: theme.text }
          }}
        >
          {readingContent}
        </Markdown>
      </ScrollView>
    <Footer/>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  readerHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingVertical: 14, borderBottomWidth: 1, marginBottom: 15 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  readerActions: { flexDirection: 'row', gap: 8 },
  actionChip: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 12, gap: 6 },
  actionChipText: { fontSize: 13, fontWeight: '700' },
  lessonTitleLarge: { fontSize: 20, fontWeight: '700', lineHeight: 30, textTransform: 'uppercase' },
});