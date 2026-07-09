import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';

const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function Settings() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [apiKey, setApiKey] = useState('');
  const [qCount, setQCount] = useState<number>(5);
  const [qStyle, setQStyle] = useState<'MCQ' | 'TF'>('MCQ');
  
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Load configuration parsing lines from key.txt
  const loadSettings = async () => {
    setLoading(true);
    try {
      const fileInfo = await FileSystem.getInfoAsync(KEY_FILE_URI);
      if (fileInfo.exists) {
        const lines = (await FileSystem.readAsStringAsync(KEY_FILE_URI)).split('\n');
        if (lines[0]) setApiKey(lines[0].trim());
        if (lines[1]) setQCount(parseInt(lines[1].trim(), 10) || 5);
        if (lines[2]) setQStyle(lines[2].trim() === 'TF' ? 'TF' : 'MCQ');
      }
    } catch (e) {
      console.error("Failed to load settings:", e);
    } finally {
      setLoading(false);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, [])
  );

  const handleSaveSettings = async () => {
    setIsSaving(true);
    try {
      const cleanKey = apiKey.trim();
      const configPayload = `${cleanKey}\n${qCount}\n${qStyle}`;
      await FileSystem.writeAsStringAsync(KEY_FILE_URI, configPayload);
      
      Alert.alert("Success", "Configuration preferences updated successfully.");
    } catch (e) {
      console.error("Failed to save preferences:", e);
      Alert.alert("Error", "Could not save configurations locally.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Configuration Settings</Text>
      </View>

      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={theme.accent} />
        </View>
      ) : (
        <ScrollView style={styles.content} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
          <Text style={[styles.sectionTitle, { color: theme.accent }]}>AI Engine Configuration</Text>
          
          <View style={[styles.configCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
            {/* API KEY INPUT */}
            <View style={styles.labelRow}>
              <FontAwesome5 name="key" size={14} color={theme.subtext} style={{ marginRight: 8 }} />
              <Text style={[styles.inputLabel, { color: theme.title }]}>Gemini API Key</Text>
            </View>
            <TextInput
              style={[styles.input, { color: theme.title, borderColor: theme.border, backgroundColor: theme.background }]}
              placeholder="Paste your Gemini API key here"
              placeholderTextColor={theme.subtext}
              value={apiKey}
              onChangeText={setApiKey}
              autoCapitalize="none"
              autoCorrect={false}
              secureTextEntry={true} 
            />
            
            {/* QUESTION COUNT SELECTOR */}
            <Text style={[styles.inputLabel, { color: theme.title, marginTop: 10 }]}>Number of Questions</Text>
            <View style={styles.segmentedControl}>
              {[5, 10, 15, 20].map((num) => (
                <Pressable
                  key={num}
                  style={[styles.segmentBtn, qCount === num && { backgroundColor: theme.accent }]}
                  onPress={() => setQCount(num)}
                >
                  <Text style={[styles.segmentText, { color: qCount === num ? '#fff' : theme.title }]}>{num}</Text>
                </Pressable>
              ))}
            </View>

            {/* QUESTION STYLE SELECTOR */}
            <Text style={[styles.inputLabel, { color: theme.title, marginTop: 10 }]}>Question Architecture Style</Text>
            <View style={styles.segmentedControl}>
              <Pressable
                style={[styles.segmentBtn, { flex: 1 }, qStyle === 'MCQ' && { backgroundColor: theme.accent }]}
                onPress={() => setQStyle('MCQ')}
              >
                <Text style={[styles.segmentText, { color: qStyle === 'MCQ' ? '#fff' : theme.title }]}>MCQ</Text>
              </Pressable>
              <Pressable
                style={[styles.segmentBtn, { flex: 1 }, qStyle === 'TF' && { backgroundColor: theme.accent }]}
                onPress={() => setQStyle('TF')}
              >
                <Text style={[styles.segmentText, { color: qStyle === 'TF' ? '#fff' : theme.title }]}>T/F Style</Text>
              </Pressable>
            </View>

            <Text style={[styles.hintText, { color: theme.subtext, marginTop: 8 }]}>
              Preferences are written locally to configure parsing pipelines for custom study metrics.
            </Text>

            <Pressable 
              style={[styles.saveBtn, { backgroundColor: theme.accent, opacity: isSaving ? 0.7 : 1 }]} 
              onPress={handleSaveSettings}
              disabled={isSaving}
            >
              {isSaving ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <>
                  <FontAwesome5 name="save" size={14} color="white" style={{ marginRight: 8 }} />
                  <Text style={styles.saveBtnText}>Save Configurations</Text>
                </>
              )}
            </Pressable>
          </View>
        </ScrollView>
      )}
      <Footer/>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 25 },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  content: { flex: 1, paddingHorizontal: 25, paddingTop: 10 },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 15, marginLeft: 5 },
  configCard: { padding: 22, borderRadius: 24, borderWidth: 1, gap: 12 },
  labelRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 2 },
  inputLabel: { fontSize: 14, fontWeight: '700' },
  input: { height: 48, borderRadius: 14, borderWidth: 1, paddingHorizontal: 16, fontSize: 14 },
  segmentedControl: { flexDirection: 'row', backgroundColor: 'rgba(0,0,0,0.03)', borderRadius: 12, padding: 4, gap: 4 },
  segmentBtn: { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: 8 },
  segmentText: { fontSize: 13, fontWeight: '600' },
  hintText: { fontSize: 12, lineHeight: 18, opacity: 0.8 },
  saveBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 10 },
  saveBtnText: { color: 'white', fontWeight: '700', fontSize: 15 }
});