import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Text, TouchableOpacity, View, useColorScheme } from 'react-native';
import Markdown from 'react-native-markdown-display';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import { Colors } from './constants/theme'; // Adjust relative path to match your layout

const QUESTIONS_DIR = `${FileSystem.documentDirectory}questions/`;
const SUMMARY_DIR = `${FileSystem.documentDirectory}summaries/`;
const FALLBACK_GEMINI_API_KEY = ""; 
const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function LessonSummaryScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ filename: string; lesson: string }>();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [loading, setLoading] = useState(true);
  const [loadingStatus, setLoadingStatus] = useState("Checking context...");
  const [summaryContent, setSummaryContent] = useState<string>("");

  // Targeted file URI for the summary cache
  const summaryFilename = `summary-${params.filename}`;
  const summaryFileUri = `${SUMMARY_DIR}${summaryFilename}`;
  const sourceFileUri = `${QUESTIONS_DIR}${params.filename}`;

  useEffect(() => {
    const fetchOrGenerateSummary = async () => {
      if (!params.filename) {
        Alert.alert("Error", "Missing module context.");
        router.back();
        return;
      }

      try {
        const summaryDirCheck = await FileSystem.getInfoAsync(SUMMARY_DIR);
            if (!summaryDirCheck.exists) {
                await FileSystem.makeDirectoryAsync(SUMMARY_DIR, { intermediates: true });
        }
        // 1. Check if summary already exists locally
        const summaryCheck = await FileSystem.getInfoAsync(summaryFileUri);
        if (summaryCheck.exists) {
          const cachedSummary = await FileSystem.readAsStringAsync(summaryFileUri);
          setSummaryContent(cachedSummary);
          setLoading(false);
          return;
        }

        // 2. Summary does not exist. Fetch source content to build one
        setLoadingStatus("Reading source content...");
        const sourceCheck = await FileSystem.getInfoAsync(sourceFileUri);
        if (!sourceCheck.exists) {
          Alert.alert("Error", "Original source text could not be located.");
          router.back();
          return;
        }
        const sourceText = await FileSystem.readAsStringAsync(sourceFileUri);

        // 3. Extract active configurations/Keys
        setLoadingStatus("Connecting to AI engine...");
        let activeApiKey = FALLBACK_GEMINI_API_KEY;
        try {
          const keyFileCheck = await FileSystem.getInfoAsync(KEY_FILE_URI);
          if (keyFileCheck.exists) {
            const lines = (await FileSystem.readAsStringAsync(KEY_FILE_URI)).split('\n');
            if (lines[0]) activeApiKey = lines[0].trim();
          }
        } catch (e) {
          console.warn("Using baseline fallback setup parameters.", e);
        }

        // 4. Fire prompt optimized to generate structural markdown elements
        const prompt = `You are a world-class academic summarizer. Read the following source text and provide a comprehensive, clear, and highly structured summary. Use Markdown features natively: clear Headings (# and ##), bulleted takeaway lists, bold terms for critical definitions, and short focused paragraphs. Avoid code block syntax envelopes or wrappers—just send the raw Markdown text.\n\nSource Text:\n${sourceText}`;

        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${activeApiKey}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }]
          })
        });

        const resData = await response.json();
        const rawSummaryMarkdown = resData.candidates[0].content.parts[0].text;

        // 5. Cache the generated summary on the local filesystem
        await FileSystem.writeAsStringAsync(summaryFileUri, rawSummaryMarkdown);
        setSummaryContent(rawSummaryMarkdown);

      } catch (error) {
        console.error("Summary processing failure:", error);
        Alert.alert("Generation Failed", "Could not structure a summary. Please check your network connection.");
        router.back();
      } finally {
        setLoading(false);
      }
    };

    fetchOrGenerateSummary();
  }, [params.filename]);

  if (loading) {
    return (
      <SafeAreaView style={[styles.container, { backgroundColor: theme.background, justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color={theme.accent} />
        <Text style={{ color: theme.subtext, marginTop: 14, fontWeight: '500' }}>{loadingStatus}</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      {/* Structural Top Header Context Bar */}
      <View style={[styles.headerContainer, { borderBottomColor: theme.border }]}>
        <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.text} />
          <Text style={[styles.headerTitle, { color: theme.text }]} numberOfLines={1}>
            {params.lesson} Summary
          </Text>
        </TouchableOpacity>
      </View>

      {/* Structured Content Layout Wrapper */}
      <ScrollView 
        style={styles.scroll} 
        showsVerticalScrollIndicator={false} 
        contentContainerStyle={styles.contentContainer}
      >
        <Markdown
          style={{
            body: { color: theme.text, fontSize: 16, lineHeight: 26 },
            heading1: { color: theme.text, fontWeight: '700', marginVertical: 14, paddingBottom: 4 },
            heading2: { color: theme.text, fontWeight: '600', marginTop: 16, marginBottom: 8 },
            paragraph: { marginVertical: 8 },
            strong: { fontWeight: '700', color: theme.text },
            bullet_list: { color: theme.text, marginVertical: 6 },
            ordered_list: { color: theme.text, marginVertical: 6 },
            list_item: { marginVertical: 4 },
          }}
        >
          {summaryContent}
        </Markdown>
      </ScrollView>
    <Footer/>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  headerContainer: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, paddingVertical: 16, borderBottomWidth: 1 },
  backBtn: { flexDirection: 'row', alignItems: 'center', gap: 10, flex: 1 },
  headerTitle: { fontSize: 18, fontWeight: '700', textTransform: 'capitalize', flex: 1 },
  scroll: { flex: 1 },
  contentContainer: { paddingHorizontal: 24, paddingTop: 16, paddingBottom: 80 }
});