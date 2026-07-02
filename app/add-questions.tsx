import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

export default function AddQuestions() {
  const [subject, setSubject] = useState('');
  const [term, setTerm] = useState('');
  const [lesson, setLesson] = useState('');
  const [jsonString, setJsonString] = useState('');
  
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const handleSave = async () => {
    if (!subject.trim() || !term.trim() || !lesson.trim() || !jsonString.trim()) {
      Alert.alert("Error", "Please fill in all meta fields and provide the questions data.");
      return;
    }

    try {
      const parsedData = JSON.parse(jsonString);

      const payload = {
        subject: subject.trim(),
        term: term.trim(),
        lesson: lesson.trim(),
        multiple_choice_questions: parsedData.multiple_choice_questions || [],
        short_answer_questions: parsedData.short_answer_questions || [],
        structured_essay_questions: parsedData.structured_essay_questions || []
      };

      const folderPath = `${FileSystem.documentDirectory}questions/`;
      const fileName = `${lesson.trim().replace(/\s+/g, '_')}-${subject.trim().replace(/\s+/g, '_')}-${term.trim().replace(/\s+/g, '_')}.json`.toLowerCase();
      const fileUri = `${folderPath}${fileName}`;

      const folderInfo = await FileSystem.getInfoAsync(folderPath);
      if (!folderInfo.exists) {
        await FileSystem.makeDirectoryAsync(folderPath, { intermediates: true });
      }

      await FileSystem.writeAsStringAsync(fileUri, JSON.stringify(payload, null, 2));
      
      Alert.alert("Success", "Question engine files compiled perfectly.", [
        { text: "OK", onPress: () => router.back() }
      ]);
    } catch (error) {
      Alert.alert("JSON Format Error", "Failed to compile. Ensure the incoming text matches the specified JSON object structure.");
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Pressable style={[styles.backBtn, { backgroundColor: theme.card }]} onPress={() => router.back()}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Import Engine Data</Text>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={{ paddingBottom: 50 }} showsVerticalScrollIndicator={false}>
        <Text style={[styles.label, { color: theme.accent }]}>Metadata Configuration</Text>
        
        <TextInput 
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Subject Name (e.g., Physiology)"
          placeholderTextColor={theme.subtext}
          value={subject}
          onChangeText={setSubject}
        />
        <TextInput 
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Term Name (e.g., Term 2)"
          placeholderTextColor={theme.subtext}
          value={term}
          onChangeText={setTerm}
        />
        <TextInput 
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Lesson / Topic (e.g., Cardiovascular)"
          placeholderTextColor={theme.subtext}
          value={lesson}
          onChangeText={setLesson}
        />

        <Text style={[styles.label, { color: theme.accent, marginTop: 15 }]}>Gemini Output Data (JSON String)</Text>
        <TextInput 
          style={[styles.textArea, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          placeholder="Paste full JSON schematic here..."
          placeholderTextColor={theme.subtext}
          multiline
          numberOfLines={12}
          textAlignVertical="top"
          value={jsonString}
          onChangeText={setJsonString}
        />

        <Pressable style={[styles.submitBtn, { backgroundColor: theme.accent }]} onPress={handleSave}>
          <Text style={styles.submitBtnText}>Compile & Commit File</Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 25, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  scroll: { flex: 1, paddingHorizontal: 25 },
  label: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12 },
  input: { height: 54, borderRadius: 16, borderWidth: 1, paddingHorizontal: 16, marginBottom: 15, fontSize: 15, fontWeight: '500' },
  textArea: { minHeight: 220, borderRadius: 20, borderWidth: 1, padding: 16, fontSize: 13, fontFamily: 'monospace', marginBottom: 25 },
  submitBtn: { height: 56, borderRadius: 20, justifyContent: 'center', alignItems: 'center', marginTop: 10 },
  submitBtnText: { color: 'white', fontSize: 16, fontWeight: '700' }
});