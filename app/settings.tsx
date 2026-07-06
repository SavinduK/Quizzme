import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

const KEY_FILE_URI = `${FileSystem.documentDirectory}key.txt`;

export default function Settings() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [apiKey, setApiKey] = useState('');
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  // Load the stored API key on focus
  const loadApiKey = async () => {
    setLoading(true);
    try {
      const fileInfo = await FileSystem.getInfoAsync(KEY_FILE_URI);
      if (fileInfo.exists) {
        const storedKey = await FileSystem.readAsStringAsync(KEY_FILE_URI);
        setApiKey(storedKey.trim());
      }
    } catch (e) {
      console.error("Failed to load API key:", e);
    } finally {
      setLoading(false);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadApiKey();
    }, [])
  );

  // Persist the updated API key back to key.txt
  const handleSaveKey = async () => {
    setIsSaving(true);
    try {
      // Ensure the text isn't just whitespace before trimming
      const cleanKey = apiKey.trim();
      await FileSystem.writeAsStringAsync(KEY_FILE_URI, cleanKey);
      
      Alert.alert("Success", "Gemini API key updated successfully.");
    } catch (e) {
      console.error("Failed to save API key:", e);
      Alert.alert("Error", "Could not save the API key to local storage.");
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      {/* Header section matching previous setup */}
      <View style={styles.header}>
        <Pressable style={[styles.backBtn, { backgroundColor: theme.card }]} onPress={() => router.replace("/")}>
          <FontAwesome5 name="arrow-left" size={16} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>Configuration Settings</Text>
      </View>

      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={theme.accent} />
        </View>
      ) : (
        <View style={styles.content}>
          <Text style={[styles.sectionTitle, { color: theme.accent }]}>AI Engine Configuration</Text>
          
          <View style={[styles.configCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
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
            
            <Text style={[styles.hintText, { color: theme.subtext }]}>
              The token is stored locally on this device within 'key.txt' and utilized to compile study decks dynamically.
            </Text>

            <Pressable 
              style={[styles.saveBtn, { backgroundColor: theme.accent, opacity: isSaving ? 0.7 : 1 }]} 
              onPress={handleSaveKey}
              disabled={isSaving}
            >
              {isSaving ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <>
                  <FontAwesome5 name="save" size={14} color="white" style={{ marginRight: 8 }} />
                  <Text style={styles.saveBtnText}>Save Key</Text>
                </>
              )}
            </Pressable>
          </View>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', padding: 25, gap: 20 },
  backBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  headerTitle: { fontSize: 24, fontWeight: '800', letterSpacing: -0.5 },
  content: { flex: 1, paddingHorizontal: 25, paddingTop: 10 },
  centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 15, marginLeft: 5 },
  configCard: { padding: 22, borderRadius: 24, borderWidth: 1, gap: 12 },
  labelRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 2 },
  inputLabel: { fontSize: 15, fontWeight: '700' },
  input: { height: 48, borderRadius: 14, borderWidth: 1, paddingHorizontal: 16, fontSize: 14, fontFamily: 'monospace' },
  hintText: { fontSize: 12, lineHeight: 18, opacity: 0.8, marginBottom: 6 },
  saveBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  saveBtnText: { color: 'white', fontWeight: '700', fontSize: 15 }
});