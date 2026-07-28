import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  useColorScheme,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import Header from './components/header';
import { Colors } from './constants/theme';

const SETTINGS_FILE_URI = `${FileSystem.documentDirectory}settings.json`;

export interface ApiKeyItem {
  id: string;
  label: string;
  key: string;
}

export type GeminiModel = 'gemini-2.5-flash' | 'gemini-2.5-pro' | 'gemini-3.5-flash' | 'gemini-3.5-pro';

export interface SettingsData {
  apiKeys: ApiKeyItem[];
  activeKeyId: string;
  selectedModel: GeminiModel;
  qCount: number;
  qStyle: 'MCQ' | 'TF' | 'SA' | 'SEQ';
  customPrompt: string;
}

const AVAILABLE_MODELS: { label: string; value: GeminiModel }[] = [
  { label: 'Gemini 2.5 Flash', value: 'gemini-2.5-flash' },
  { label: 'Gemini 2.5 Pro', value: 'gemini-2.5-pro' },
  { label: 'Gemini 3.5 Flash', value: 'gemini-3.5-flash' },
  { label: 'Gemini 3.5 Pro', value: 'gemini-3.5-pro' },
];

export default function Settings() {
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // State Management
  const [apiKeys, setApiKeys] = useState<ApiKeyItem[]>([]);
  const [activeKeyId, setActiveKeyId] = useState<string>('');
  const [selectedModel, setSelectedModel] = useState<GeminiModel>('gemini-2.5-flash');
  const [qCount, setQCount] = useState<number>(5);
  const [qStyle, setQStyle] = useState<'MCQ' | 'TF' | 'SA' | 'SEQ'>('MCQ');
  const [customPrompt, setCustomPrompt] = useState<string>('');
  
  // New Key Modal & Inputs State
  const [isAddKeyModalVisible, setIsAddKeyModalVisible] = useState(false);
  const [newKeyLabel, setNewKeyLabel] = useState('');
  const [newKeyValue, setNewKeyValue] = useState('');
  const [showApiKeyMap, setShowApiKeyMap] = useState<Record<string, boolean>>({});

  const [isSaving, setIsSaving] = useState(false);

  // Accordion Sections
  const [aiExpanded, setAiExpanded] = useState(true);
  const [quizExpanded, setQuizExpanded] = useState(true);

  // Alert Modal
  const [alertVisible, setAlertVisible] = useState(false);
  const [alertData, setAlertData] = useState({ title: '', message: '', type: 'success' });

  const showAlert = (title: string, message: string, type: 'success' | 'error' = 'success') => {
    setAlertData({ title, message, type });
    setAlertVisible(true);
  };

  // Load JSON Settings
  const loadSettings = async () => {
    try {
      const fileInfo = await FileSystem.getInfoAsync(SETTINGS_FILE_URI);
      if (fileInfo.exists) {
        const content = await FileSystem.readAsStringAsync(SETTINGS_FILE_URI);
        const parsed: SettingsData = JSON.parse(content);

        if (Array.isArray(parsed.apiKeys)) setApiKeys(parsed.apiKeys);
        if (parsed.activeKeyId) setActiveKeyId(parsed.activeKeyId);
        if (parsed.selectedModel) setSelectedModel(parsed.selectedModel);
        if (parsed.qCount) setQCount(parsed.qCount);
        if (parsed.qStyle) setQStyle(parsed.qStyle);
        if (parsed.customPrompt !== undefined) setCustomPrompt(parsed.customPrompt);
      }
    } catch (e) {
      console.error('Failed to load settings:', e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadSettings();
    }, [])
  );

  // Save Settings as Structured JSON
  const handleSaveSettings = async () => {
    setIsSaving(true);
    try {
      const payload: SettingsData = {
        apiKeys,
        activeKeyId,
        selectedModel,
        qCount,
        qStyle,
        customPrompt: customPrompt.trim(),
      };

      await FileSystem.writeAsStringAsync(SETTINGS_FILE_URI, JSON.stringify(payload, null, 2));
      showAlert('Success', 'Configuration settings saved successfully.', 'success');
    } catch (e) {
      console.error('Failed to save settings:', e);
      showAlert('Error', 'Could not save configurations locally.', 'error');
    } finally {
      setIsSaving(false);
    }
  };

  // Key Actions
  const handleAddKey = () => {
    if (!newKeyValue.trim()) {
      showAlert('Missing Input', 'Please enter a valid API Key.', 'error');
      return;
    }

    const newId = `key_${Date.now()}`;
    const newEntry: ApiKeyItem = {
      id: newId,
      label: newKeyLabel.trim() || `Key ${apiKeys.length + 1}`,
      key: newKeyValue.trim(),
    };

    const updatedKeys = [...apiKeys, newEntry];
    setApiKeys(updatedKeys);

    // If no key is currently active, set this new key as active
    if (!activeKeyId) {
      setActiveKeyId(newId);
    }

    setNewKeyLabel('');
    setNewKeyValue('');
    setIsAddKeyModalVisible(false);
  };

  const handleDeleteKey = (id: string) => {
    const filtered = apiKeys.filter((item) => item.id !== id);
    setApiKeys(filtered);
    if (activeKeyId === id) {
      setActiveKeyId(filtered.length > 0 ? filtered[0].id : '');
    }
  };

  const toggleVisibility = (id: string) => {
    setShowApiKeyMap((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <Header title="Configuration Settings" />

      <KeyboardAvoidingView
        style={styles.keyboardView}
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.content}>

            {/* SECTION 1: AI ENGINE & KEYS */}
            <Pressable
              style={styles.accordionHeader}
              onPress={() => setAiExpanded(!aiExpanded)}
            >
              <Text style={[styles.sectionTitle, { color: theme.accent }]}>AI Engine & Credentials</Text>
              <FontAwesome5
                name={aiExpanded ? 'chevron-up' : 'chevron-down'}
                size={12}
                color={theme.accent}
              />
            </Pressable>

            {aiExpanded && (
              <View style={styles.configCard}>

                {/* MODEL SELECTOR BUTTONS */}
                <View style={styles.settingRow}>
                  <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>Select Gemini Model</Text>
                  <View style={styles.gridContainer}>
                    <View style={styles.buttonOptionRow}>
                      {AVAILABLE_MODELS.slice(0, 2).map((item) => (
                        <Pressable
                          key={item.value}
                          style={[
                            styles.normalOptionBtn,
                            styles.flexButton,
                            { borderColor: theme.border },
                            selectedModel === item.value && { backgroundColor: theme.buttons, borderColor: theme.accent }
                          ]}
                          onPress={() => setSelectedModel(item.value)}
                        >
                          <Text style={[styles.normalOptionText, { color: selectedModel === item.value ? '#fff' : theme.title }]}>
                            {item.label}
                          </Text>
                        </Pressable>
                      ))}
                    </View>
                    <View style={styles.buttonOptionRow}>
                      {AVAILABLE_MODELS.slice(2, 4).map((item) => (
                        <Pressable
                          key={item.value}
                          style={[
                            styles.normalOptionBtn,
                            styles.flexButton,
                            { borderColor: theme.border },
                            selectedModel === item.value && { backgroundColor: theme.buttons, borderColor: theme.accent }
                          ]}
                          onPress={() => setSelectedModel(item.value)}
                        >
                          <Text style={[styles.normalOptionText, { color: selectedModel === item.value ? '#fff' : theme.title }]}>
                            {item.label}
                          </Text>
                        </Pressable>
                      ))}
                    </View>
                  </View>
                </View>

                {/* API KEYS LIST WITH HEADER ADD BUTTON */}
                <View style={styles.settingRow}>
                  <View style={styles.sectionHeaderRow}>
                    <Text style={[styles.inputLabel, { color: theme.title }]}>API Keys</Text>
                    <Pressable
                      style={[styles.headerAddBtn, { backgroundColor: theme.buttons, borderColor: theme.accent }]}
                      onPress={() => setIsAddKeyModalVisible(true)}
                    >
                      <FontAwesome5 name="plus" size={10} color={theme.accent} style={{ marginRight: 4 }} />
                      <Text style={[styles.headerAddBtnText, { color: theme.accent }]}>Add Key</Text>
                    </Pressable>
                  </View>
                  
                  {apiKeys.length === 0 ? (
                    <Text style={[styles.subText, { color: theme.subtext, marginTop: 4, marginBottom: 10 }]}>
                      No keys added yet. Click "+ Add Key" above to include one.
                    </Text>
                  ) : (
                    apiKeys.map((item) => {
                      const isActive = activeKeyId === item.id;
                      const isVisible = !!showApiKeyMap[item.id];
                      return (
                        <Pressable
                          key={item.id}
                          style={[
                            styles.keyCard,
                            { borderColor: isActive ? theme.accent : theme.border, backgroundColor: theme.background }
                          ]}
                          onPress={() => setActiveKeyId(item.id)}
                        >
                          <View style={styles.keyHeaderRow}>
                            <View style={styles.keyTitleGroup}>
                              <FontAwesome5
                                name={isActive ? 'check-circle' : 'circle'}
                                size={14}
                                color={isActive ? theme.accent : theme.subtext}
                                style={{ marginRight: 8 }}
                              />
                              <Text style={[styles.keyLabel, { color: theme.title }]}>{item.label}</Text>
                              {isActive && (
                                <View style={[styles.activeBadge, { backgroundColor: theme.accent }]}>
                                  <Text style={styles.activeBadgeText}>ACTIVE</Text>
                                </View>
                              )}
                            </View>

                            <Pressable onPress={() => handleDeleteKey(item.id)} hitSlop={10}>
                              <FontAwesome5 name="trash-alt" size={14} color="#D8000C" />
                            </Pressable>
                          </View>

                          <View style={styles.keyDisplayRow}>
                            <Text style={[styles.keyText, { color: theme.subtext }]}>
                              {isVisible ? item.key : '••••••••••••••••••••' + item.key.slice(-4)}
                            </Text>
                            <Pressable onPress={() => toggleVisibility(item.id)} hitSlop={8}>
                              <FontAwesome5 name={isVisible ? 'eye' : 'eye-slash'} size={13} color={theme.subtext} />
                            </Pressable>
                          </View>
                        </Pressable>
                      );
                    })
                  )}
                </View>

              </View>
            )}

            {/* SECTION SEPARATOR */}
            <View style={[styles.horizontalBar, { backgroundColor: theme.border }]} />

            {/* SECTION 2: QUIZ GENERATION SETTINGS */}
            <Pressable
              style={styles.accordionHeader}
              onPress={() => setQuizExpanded(!quizExpanded)}
            >
              <Text style={[styles.sectionTitle, { color: theme.accent }]}>Quiz Settings</Text>
              <FontAwesome5
                name={quizExpanded ? 'chevron-up' : 'chevron-down'}
                size={12}
                color={theme.accent}
              />
            </Pressable>

            {quizExpanded && (
              <View style={styles.configCard}>

                {/* QUESTION COUNT */}
                <View style={styles.settingRow}>
                  <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>Number of Questions</Text>
                  <View style={styles.buttonOptionRow}>
                    {[5, 10, 15, 20].map((num) => (
                      <Pressable
                        key={num}
                        style={[
                          styles.normalOptionBtn,
                          styles.flexButton,
                          { borderColor: theme.border },
                          qCount === num && { backgroundColor: theme.buttons, borderColor: theme.accent }
                        ]}
                        onPress={() => setQCount(num)}
                      >
                        <Text style={[styles.normalOptionText, { color: qCount === num ? '#fff' : theme.title }]}>
                          {num}
                        </Text>
                      </Pressable>
                    ))}
                  </View>
                </View>

                {/* QUESTION TYPE */}
                <View style={styles.settingRow}>
                  <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>Question Type</Text>
                  <View style={styles.gridContainer}>
                    <View style={styles.buttonOptionRow}>
                      <Pressable
                        style={[
                          styles.normalOptionBtn,
                          styles.flexButton,
                          { borderColor: theme.border },
                          qStyle === 'MCQ' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                        ]}
                        onPress={() => setQStyle('MCQ')}
                      >
                        <Text style={[styles.normalOptionText, { color: qStyle === 'MCQ' ? '#fff' : theme.title }]}>MCQ</Text>
                      </Pressable>

                      <Pressable
                        style={[
                          styles.normalOptionBtn,
                          styles.flexButton,
                          { borderColor: theme.border },
                          qStyle === 'TF' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                        ]}
                        onPress={() => setQStyle('TF')}
                      >
                        <Text style={[styles.normalOptionText, { color: qStyle === 'TF' ? '#fff' : theme.title }]}>True / False</Text>
                      </Pressable>
                    </View>

                    <View style={styles.buttonOptionRow}>
                      <Pressable
                        style={[
                          styles.normalOptionBtn,
                          styles.flexButton,
                          { borderColor: theme.border },
                          qStyle === 'SA' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                        ]}
                        onPress={() => setQStyle('SA')}
                      >
                        <Text style={[styles.normalOptionText, { color: qStyle === 'SA' ? '#fff' : theme.title }]}>Short Answer</Text>
                      </Pressable>

                      <Pressable
                        style={[
                          styles.normalOptionBtn,
                          styles.flexButton,
                          { borderColor: theme.border },
                          qStyle === 'SEQ' && { backgroundColor: theme.buttons, borderColor: theme.accent }
                        ]}
                        onPress={() => setQStyle('SEQ')}
                      >
                        <Text style={[styles.normalOptionText, { color: qStyle === 'SEQ' ? '#fff' : theme.title }]}>Structured</Text>
                      </Pressable>
                    </View>
                  </View>
                </View>

                {/* CUSTOM PROMPT */}
                <View style={styles.settingRow}>
                  <Text style={[styles.inputLabel, { color: theme.title, marginBottom: 8 }]}>
                    Custom Generation Instructions
                  </Text>
                  <TextInput
                    style={[styles.textArea, { color: theme.title, borderColor: theme.border, backgroundColor: theme.background }]}
                    placeholder="e.g., Focus heavily on clinical diagnostics..."
                    placeholderTextColor={theme.subtext}
                    value={customPrompt}
                    onChangeText={setCustomPrompt}
                    multiline
                    numberOfLines={3}
                    textAlignVertical="top"
                  />
                </View>

                {/* SAVE BUTTON */}
                <Pressable
                  style={[
                    styles.saveBtn,
                    { backgroundColor: theme.buttons, opacity: isSaving ? 0.7 : 1, borderColor: theme.accent, borderWidth: 1 }
                  ]}
                  onPress={handleSaveSettings}
                  disabled={isSaving}
                >
                  {isSaving ? (
                    <ActivityIndicator size="small" color="white" />
                  ) : (
                    <>
                      <FontAwesome5 name="save" size={14} color={theme.accent} style={{ marginRight: 8 }} />
                      <Text style={[styles.saveBtnText, { color: theme.accent }]}>Save Configurations</Text>
                    </>
                  )}
                </Pressable>

              </View>
            )}

          </View>
        </ScrollView>
      </KeyboardAvoidingView>

      <Footer />

      {/* ADD API KEY MODAL */}
      <Modal
        animationType="fade"
        transparent
        visible={isAddKeyModalVisible}
        onRequestClose={() => setIsAddKeyModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.background, borderColor: theme.border }]}>
            <View style={styles.modalHeaderRow}>
              <FontAwesome5 name="key" size={18} color={theme.accent} style={{ marginRight: 10 }} />
              <Text style={[styles.modalTitle, { color: theme.title }]}>Add New API Key</Text>
            </View>

            <View style={{ gap: 12, marginVertical: 16 }}>
              <TextInput
                style={[styles.textInput, { color: theme.title, borderColor: theme.border }]}
                placeholder="Key Label (e.g., Main Key)"
                placeholderTextColor={theme.subtext}
                value={newKeyLabel}
                onChangeText={setNewKeyLabel}
              />

              <TextInput
                style={[styles.textInput, { color: theme.title, borderColor: theme.border }]}
                placeholder="Paste Gemini API Key"
                placeholderTextColor={theme.subtext}
                value={newKeyValue}
                onChangeText={setNewKeyValue}
                autoCapitalize="none"
                autoCorrect={false}
                secureTextEntry
              />
            </View>

            <View style={{ flexDirection: 'row', gap: 10 }}>
              <Pressable
                style={[styles.modalActionBtn, { borderColor: theme.border, borderWidth: 1, flex: 1 }]}
                onPress={() => setIsAddKeyModalVisible(false)}
              >
                <Text style={[styles.modalActionBtnText, { color: theme.subtext }]}>Cancel</Text>
              </Pressable>

              <Pressable
                style={[styles.modalActionBtn, { backgroundColor: theme.buttons, borderColor: theme.accent, borderWidth: 1, flex: 1 }]}
                onPress={handleAddKey}
              >
                <Text style={[styles.modalActionBtnText, { color: theme.accent }]}>Save Key</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>

      {/* ALERT MODAL */}
      <Modal
        animationType="fade"
        transparent
        visible={alertVisible}
        onRequestClose={() => setAlertVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.background, borderColor: theme.border }]}>
            <View style={styles.modalHeaderRow}>
              <FontAwesome5
                name={alertData.type === 'success' ? 'check-circle' : 'exclamation-circle'}
                size={20}
                color={alertData.type === 'success' ? '#4BB543' : '#D8000C'}
                style={{ marginRight: 10 }}
              />
              <Text style={[styles.modalTitle, { color: theme.title }]}>{alertData.title}</Text>
            </View>
            <Text style={[styles.modalMessage, { color: theme.subtext }]}>{alertData.message}</Text>

            <Pressable
              style={[styles.modalCloseBtn, { backgroundColor: theme.buttons, borderColor: theme.accent }]}
              onPress={() => setAlertVisible(false)}
            >
              <Text style={[styles.modalCloseBtnText, { color: theme.accent }]}>Dismiss</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  keyboardView: { flex: 1 },
  scrollContent: { flexGrow: 1 },
  content: { flex: 1, paddingHorizontal: 25, paddingVertical: 20 },
  accordionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
    paddingRight: 5,
  },
  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginLeft: 5 },
  horizontalBar: { height: 1, width: '100%', marginVertical: 14, opacity: 0.6 },
  configCard: { paddingVertical: 10, paddingHorizontal: 5, gap: 16 },
  settingRow: { gap: 4 },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  headerAddBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 8,
    borderWidth: 1,
  },
  headerAddBtnText: {
    fontSize: 12,
    fontWeight: '700',
  },
  subText: { fontSize: 12 },
  inputLabel: { fontSize: 14, fontWeight: '700', letterSpacing: -0.1 },

  // Key Card Styles
  keyCard: {
    borderRadius: 14,
    borderWidth: 1.5,
    padding: 12,
    marginBottom: 8,
    gap: 8,
  },
  keyHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  keyTitleGroup: { flexDirection: 'row', alignItems: 'center' },
  keyLabel: { fontSize: 14, fontWeight: '700' },
  activeBadge: {
    marginLeft: 8,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 6,
  },
  activeBadgeText: { color: '#fff', fontSize: 9, fontWeight: '800' },
  keyDisplayRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  keyText: { fontSize: 12, fontFamily: Platform.OS === 'ios' ? 'Courier' : 'monospace' },

  textInput: {
    height: 44,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 12,
    fontSize: 13,
  },

  // Layout Controls
  flexButton: { flex: 1 },
  gridContainer: { gap: 8, width: '100%' },
  buttonOptionRow: { flexDirection: 'row', gap: 8, width: '100%' },
  normalOptionBtn: {
    height: 48,
    borderRadius: 14,
    borderWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.01)',
  },
  normalOptionText: { fontWeight: '600', fontSize: 13 },
  textArea: { minHeight: 80, borderRadius: 14, borderWidth: 1, padding: 14, fontSize: 14, lineHeight: 20 },
  saveBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 12 },
  saveBtnText: { fontWeight: '700', fontSize: 15 },

  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 30,
  },
  modalContent: {
    width: '100%',
    borderRadius: 20,
    borderWidth: 1,
    padding: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  modalHeaderRow: { flexDirection: 'row', alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '800' },
  modalMessage: { fontSize: 14, lineHeight: 20, marginBottom: 24 },
  modalCloseBtn: { height: 44, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  modalCloseBtnText: { fontWeight: '700', fontSize: 14 },
  modalActionBtn: { height: 44, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
  modalActionBtnText: { fontWeight: '700', fontSize: 14 },
});