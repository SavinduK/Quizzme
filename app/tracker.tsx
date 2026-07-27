import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useRef, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
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

// File URI for persistent task storage
const TASKS_FILE_URI = `${FileSystem.documentDirectory}work_tracker_tasks.json`;

export interface TaskItem {
  id: string;
  date: string; // YYYY-MM-DD format
  time: string;
  title: string;
  subject: string;
  completed: boolean;
  isCustom?: boolean;
}

// Initial Timetable JSON Data Structure
const INITIAL_TIMETABLE_JSON: TaskItem[] = [
  {
    id: 'tt-1',
    date: new Date().toISOString().split('T')[0], // Today
    time: '08:00 AM - 09:30 AM',
    title: 'Cardiovascular System Review',
    subject: 'Physiology',
    completed: false,
  },
  {
    id: 'tt-2',
    date: new Date().toISOString().split('T')[0],
    time: '10:00 AM - 11:00 AM',
    title: 'Complete Daily MCQ Flash-Quiz',
    subject: 'Physiology',
    completed: true,
  },
  {
    id: 'tt-3',
    date: new Date().toISOString().split('T')[0],
    time: '02:00 PM - 03:30 PM',
    title: 'Neuroanatomy Diagrams Practice',
    subject: 'Anatomy',
    completed: false,
  },
];

export default function WorkTrackerScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // ScrollView Ref for Calendar Strip
  const calendarScrollRef = useRef<ScrollView>(null);

  // Calendar State
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [weekDays, setWeekDays] = useState<Array<{ dateStr: string; dayName: string; dayNum: number }>>([]);

  // Tasks State
  const [tasks, setTasks] = useState<TaskItem[]>([]);

  // Add Task Modal State
  const [addModalVisible, setAddModalVisible] = useState<boolean>(false);
  const [newTitle, setNewTitle] = useState<string>('');
  const [newSubject, setNewSubject] = useState<string>('');
  const [newTime, setNewTime] = useState<string>('04:00 PM');

  // Import JSON Modal State
  const [importModalVisible, setImportModalVisible] = useState<boolean>(false);
  const [jsonInput, setJsonInput] = useState<string>('');

  // Delete Confirmation Modal State
  const [deleteModalVisible, setDeleteModalVisible] = useState<boolean>(false);
  const [taskToDeleteId, setTaskToDeleteId] = useState<string | null>(null);

  // Initialize Calendar & Scroll to Today
  const initializeCalendar = () => {
    const days = [];
    const today = new Date();

    for (let i = -30; i <= 30; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() + i);
      const dateStr = d.toISOString().split('T')[0];
      const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
      const dayNum = d.getDate();
      days.push({ dateStr, dayName, dayNum });
    }
    setWeekDays(days);

    // Scroll to index 30 (today) after layout calculation
    setTimeout(() => {
      const itemWidth = 58 + 10; // dateCard width (58) + gap (10)
      const targetOffset = 30 * itemWidth - 10; // Scroll offset to position today near center
      calendarScrollRef.current?.scrollTo({ x: Math.max(0, targetOffset), animated: false });
    }, 50);
  };

  const loadTasksFromStorage = async () => {
    try {
      const fileInfo = await FileSystem.getInfoAsync(TASKS_FILE_URI);
      if (fileInfo.exists) {
        const content = await FileSystem.readAsStringAsync(TASKS_FILE_URI);
        if (content) {
          setTasks(JSON.parse(content));
          return;
        }
      }
      setTasks(INITIAL_TIMETABLE_JSON);
      await FileSystem.writeAsStringAsync(
        TASKS_FILE_URI,
        JSON.stringify(INITIAL_TIMETABLE_JSON)
      );
    } catch (e) {
      console.error('Error reading task store:', e);
      setTasks(INITIAL_TIMETABLE_JSON);
    }
  };

  const saveTasksToStorage = async (updatedTasks: TaskItem[]) => {
    setTasks(updatedTasks);
    try {
      await FileSystem.writeAsStringAsync(
        TASKS_FILE_URI,
        JSON.stringify(updatedTasks)
      );
    } catch (e) {
      console.error('Error saving task store:', e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      initializeCalendar();
      loadTasksFromStorage();
    }, [])
  );

  // Toggle Task Completion
  const toggleTaskCompletion = (id: string) => {
    const updated = tasks.map((t) =>
      t.id === id ? { ...t, completed: !t.completed } : t
    );
    saveTasksToStorage(updated);
  };

  // Add New Task
  const handleAddNewTask = () => {
    if (!newTitle.trim()) {
      Alert.alert('Missing Info', 'Please enter a task title.');
      return;
    }

    const newTask: TaskItem = {
      id: `custom-${Date.now()}`,
      date: selectedDate,
      time: newTime.trim() || 'Flex Time',
      title: newTitle.trim(),
      subject: newSubject.trim() || 'General Study',
      completed: false,
      isCustom: true,
    };

    const updated = [...tasks, newTask];
    saveTasksToStorage(updated);

    // Reset Form
    setNewTitle('');
    setNewSubject('');
    setAddModalVisible(false);
  };

  // Handle JSON Schedule Import
  const handleImportJson = () => {
    if (!jsonInput.trim()) {
      Alert.alert('Empty Input', 'Please paste a valid JSON string.');
      return;
    }

    try {
      const parsedData = JSON.parse(jsonInput);
      const itemsToImport = Array.isArray(parsedData) ? parsedData : [parsedData];

      // Format/validate basic structure
      const validTasks: TaskItem[] = itemsToImport.map((item, index) => ({
        id: item.id || `imported-${Date.now()}-${index}`,
        date: item.date || selectedDate,
        time: item.time || 'Flex Time',
        title: item.title || 'Untitled Task',
        subject: item.subject || 'General',
        completed: Boolean(item.completed),
        isCustom: item.isCustom ?? true,
      }));

      const updated = [...tasks, ...validTasks];
      saveTasksToStorage(updated);

      setJsonInput('');
      setImportModalVisible(false);
    } catch (error) {
      Alert.alert('Invalid JSON', 'Please check the formatting of your JSON text and try again.');
    }
  };

  // Delete Task Prompt Trigger
  const promptDeleteTask = (id: string) => {
    setTaskToDeleteId(id);
    setDeleteModalVisible(true);
  };

  // Confirm Delete Task Action
  const confirmDeleteTask = () => {
    if (taskToDeleteId) {
      const updated = tasks.filter((t) => t.id !== taskToDeleteId);
      saveTasksToStorage(updated);
    }
    setDeleteModalVisible(false);
    setTaskToDeleteId(null);
  };

  // Filter tasks for current day
  const dayTasks = tasks.filter((t) => t.date === selectedDate);
  const completedCount = dayTasks.filter((t) => t.completed).length;

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? 'light-content' : 'dark-content'} />

      <Header title="Work Tracker"  />

      {/* --- CALENDAR STRIP --- */}
      <View style={[styles.calendarContainer, { borderBottomColor: theme.border }]}>
        <ScrollView
          ref={calendarScrollRef}
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.calendarScroll}
        >
          {weekDays.map((item) => {
            const isSelected = item.dateStr === selectedDate;
            const hasTasks = tasks.some((t) => t.date === item.dateStr);

            return (
              <Pressable
                key={item.dateStr}
                onPress={() => setSelectedDate(item.dateStr)}
                style={[
                  styles.dateCard,
                  { backgroundColor: theme.card, borderColor: isSelected ? theme.accent : theme.border },
                  isSelected && { backgroundColor: "#C084FC" },
                ]}
              >
                <Text style={[styles.dayName, { color: isSelected ? '#FFF' : theme.subtext }]}>
                  {item.dayName}
                </Text>
                <Text style={[styles.dayNum, { color: isSelected ? '#FFF' : theme.title }]}>
                  {item.dayNum}
                </Text>
                {hasTasks}
              </Pressable>
            );
          })}
        </ScrollView>
      </View>

      {/* --- DAILY PROGRESS SUMMARY --- */}
      <View style={styles.controlRow}>
        <View style={styles.summaryTextContainer}>
          <Text style={[styles.summaryLabel, { color: theme.subtext }]}>
            {selectedDate === new Date().toISOString().split('T')[0] ? "Today's Schedule" : `Schedule for ${selectedDate}`}
          </Text>
          <Text style={[styles.progressSubtitle, { color: theme.title }]}>
            {dayTasks.length > 0
              ? `${completedCount} of ${dayTasks.length} tasks completed`
              : ''}
          </Text>
        </View>

        {/* Action Buttons Group */}
        <View style={{ flexDirection: 'row', gap: 8 }}>
          <Pressable
            onPress={() => setImportModalVisible(true)}
            style={[styles.addActionButton]}
          >
            <FontAwesome5 name="file-import" size={14} color="#C084FC" />
          </Pressable>

          <Pressable
            onPress={() => setAddModalVisible(true)}
            style={[styles.addActionButton]}
          >
            <FontAwesome5 name="plus" size={14} color="#C084FC" />
          </Pressable>
        </View>
      </View>

      {/* --- TASKS LIST --- */}
      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {dayTasks.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="calendar-check" size={48} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>
              No planned tasks or timetable entries for this date.
            </Text>
          </View>
        ) : (
          dayTasks.map((item) => (
            <View
              key={item.id}
              style={[
                styles.taskCard,
                { backgroundColor: theme.card, borderColor: theme.border },
                item.completed && { opacity: 0.65 },
              ]}
            >
              {/* Completion Checkbox */}
              <Pressable
                onPress={() => toggleTaskCompletion(item.id)}
                style={[
                  styles.checkbox,
                  { borderColor: item.completed ? theme.accent : theme.border },
                  item.completed && { backgroundColor: theme.accent },
                ]}
              >
                {item.completed && <FontAwesome5 name="check" size={12} color="#FFF" />}
              </Pressable>

              {/* Task Body */}
              <View style={styles.taskBody}>
                <Text
                  style={[
                    styles.taskTitle,
                    { color: theme.title },
                    item.completed && styles.strikeText,
                  ]}
                >
                  {item.title}
                </Text>

                <Text style={[styles.taskSubject, { color: theme.subtext }]}>
                  {item.subject}
                </Text>

                {/* Time re-positioned below the subject */}
                <Text style={[styles.taskTime, { color: theme.subtext }]}>
                  <FontAwesome5 name="clock" size={11} color={theme.subtext} /> {item.time}
                </Text>
              </View>

              {/* Delete Button */}
              <Pressable
                onPress={() => promptDeleteTask(item.id)}
                style={styles.deleteButton}
                hitSlop={10}
              >
                <FontAwesome5 name="trash-alt" size={14} color="#ef4444" />
              </Pressable>
            </View>
          ))
        )}
      </ScrollView>

     {/* --- ADD TASK MODAL BOTTOM SHEET --- */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={addModalVisible}
        onRequestClose={() => setAddModalVisible(false)}
      >
        <KeyboardAvoidingView
          style={{ flex: 1 }}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
          <View style={styles.drawerOverlay}>
            <Pressable style={styles.drawerDismissZone} onPress={() => setAddModalVisible(false)} />

            <View style={[styles.drawerSheetContainer, { backgroundColor: theme.card }]}>
              <SafeAreaView edges={['bottom']}>
                <View style={[styles.drawerHeader, { borderBottomColor: theme.border }]}>
                  <Text style={[styles.drawerTitle, { color: theme.title }]}>Add Planned Task</Text>
                  <Pressable onPress={() => setAddModalVisible(false)} style={styles.drawerCloseBtn}>
                    <FontAwesome5 name="times" size={16} color={theme.title} />
                  </Pressable>
                </View>

                <ScrollView
                  style={styles.drawerBodyContent}
                  showsVerticalScrollIndicator={false}
                  keyboardShouldPersistTaps="handled"
                >
                  {/* Task Title */}
                  <Text style={[styles.groupHeadingLabel, { color: theme.subtext }]}>Title</Text>
                  <TextInput
                    style={[styles.textInput, { backgroundColor: theme.background, borderColor: theme.border, color: theme.title }]}
                    placeholder="e.g. Read Anatomy Chapter 4"
                    placeholderTextColor={theme.subtext}
                    value={newTitle}
                    onChangeText={setNewTitle}
                  />

                  {/* Subject */}
                  <Text style={[styles.groupHeadingLabel, { color: theme.subtext, marginTop: 16 }]}>Subject / Tag</Text>
                  <TextInput
                    style={[styles.textInput, { backgroundColor: theme.background, borderColor: theme.border, color: theme.title }]}
                    placeholder="e.g. Pathology"
                    placeholderTextColor={theme.subtext}
                    value={newSubject}
                    onChangeText={setNewSubject}
                  />

                  {/* Time Window */}
                  <Text style={[styles.groupHeadingLabel, { color: theme.subtext, marginTop: 16 }]}>Time Window</Text>
                  <TextInput
                    style={[styles.textInput, { backgroundColor: theme.background, borderColor: theme.border, color: theme.title }]}
                    placeholder="e.g. 03:00 PM - 04:30 PM"
                    placeholderTextColor={theme.subtext}
                    value={newTime}
                    onChangeText={setNewTime}
                  />

                  {/* Action Buttons */}
                  <View style={styles.drawerFooterGroup}>
                    <Pressable
                      onPress={() => setAddModalVisible(false)}
                      style={[styles.footerBtnSecondary, { borderColor: theme.border }]}
                    >
                      <Text style={{ color: theme.title, fontWeight: '600' }}>Cancel</Text>
                    </Pressable>
                    <Pressable
                      onPress={handleAddNewTask}
                      style={[styles.footerBtnPrimary, { backgroundColor: theme.accent }]}
                    >
                      <Text style={{ color: '#FFF', fontWeight: '700' }}>Save Task</Text>
                    </Pressable>
                  </View>
                </ScrollView>
              </SafeAreaView>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      {/* --- +IMPORT JSON MODAL BOTTOM SHEET --- */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={importModalVisible}
        onRequestClose={() => setImportModalVisible(false)}
      >
        <KeyboardAvoidingView
          style={{ flex: 1 }}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
          <View style={styles.drawerOverlay}>
            <Pressable style={styles.drawerDismissZone} onPress={() => setImportModalVisible(false)} />

            <View style={[styles.drawerSheetContainer, { backgroundColor: theme.card }]}>
              <SafeAreaView edges={['bottom']}>
                <View style={[styles.drawerHeader, { borderBottomColor: theme.border }]}>
                  <Text style={[styles.drawerTitle, { color: theme.title }]}>Import Tasks via JSON</Text>
                  <Pressable onPress={() => setImportModalVisible(false)} style={styles.drawerCloseBtn}>
                    <FontAwesome5 name="times" size={16} color={theme.title} />
                  </Pressable>
                </View>

                <ScrollView
                  style={styles.drawerBodyContent}
                  showsVerticalScrollIndicator={false}
                  keyboardShouldPersistTaps="handled"
                >
                  <Text style={[styles.groupHeadingLabel, { color: theme.subtext }]}>JSON Schedule Payload</Text>
                  <TextInput
                    style={[
                      styles.textInput,
                      {
                        backgroundColor: theme.background,
                        borderColor: theme.border,
                        color: theme.title,
                        height: 120,
                        textAlignVertical: 'top',
                        paddingTop: 10,
                      },
                    ]}
                    placeholder='Paste JSON array, e.g. [{"title": "Study", "subject": "Math", "time": "10:00 AM"}]'
                    placeholderTextColor={theme.subtext}
                    value={jsonInput}
                    onChangeText={setJsonInput}
                    multiline={true}
                  />

                  <View style={styles.drawerFooterGroup}>
                    <Pressable
                      onPress={() => setImportModalVisible(false)}
                      style={[styles.footerBtnSecondary, { borderColor: theme.border }]}
                    >
                      <Text style={{ color: theme.title, fontWeight: '600' }}>Cancel</Text>
                    </Pressable>
                    <Pressable
                      onPress={handleImportJson}
                      style={[styles.footerBtnPrimary, { backgroundColor: theme.accent }]}
                    >
                      <Text style={{ color: '#FFF', fontWeight: '700' }}>Import JSON</Text>
                    </Pressable>
                  </View>
                </ScrollView>
              </SafeAreaView>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      {/* --- CENTERED POPUP DELETE CONFIRMATION MODAL --- */}
      <Modal
        animationType="fade"
        transparent={true}
        visible={deleteModalVisible}
        onRequestClose={() => setDeleteModalVisible(false)}
      >
        <View style={styles.popupOverlay}>
          <Pressable style={styles.drawerDismissZone} onPress={() => setDeleteModalVisible(false)} />

          <View style={[styles.popupCardContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
            <View style={{ alignItems: 'center', marginBottom: 16 }}>
              <View style={styles.deleteIconBadge}>
                <FontAwesome5 name="trash-alt" size={20} color="#ef4444" />
              </View>
              <Text style={[styles.drawerTitle, { color: theme.title, fontSize: 18, marginTop: 12 }]}>Delete Task</Text>
              <Text style={[styles.emptyText, { color: theme.subtext, marginTop: 8 }]}>
                Are you sure you want to remove this task? This action cannot be undone.
              </Text>
            </View>

            <View style={[styles.drawerFooterGroup, { marginTop: 8, marginBottom: 0 }]}>
              <Pressable
                onPress={() => setDeleteModalVisible(false)}
                style={[styles.footerBtnSecondary, { borderColor: theme.border }]}
              >
                <Text style={{ color: theme.title, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable
                onPress={confirmDeleteTask}
                style={[styles.footerBtnPrimary, { backgroundColor: '#ef4444' }]}
              >
                <Text style={{ color: '#FFF', fontWeight: '700' }}>Delete</Text>
              </Pressable>
            </View>
          </View>

          <Pressable style={styles.drawerDismissZone} onPress={() => setDeleteModalVisible(false)} />
        </View>
      </Modal>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  calendarContainer: { borderBottomWidth: 1, paddingVertical: 12 },
  calendarScroll: { paddingHorizontal: 16, gap: 10 },
  dateCard: {
    width: 58,
    height: 70,
    borderRadius: 16,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 8,
  },
  dayName: { fontSize: 12, fontWeight: '600', textTransform: 'uppercase' },
  dayNum: { fontSize: 18, fontWeight: '700', marginTop: 2 },
  taskDot: { width: 5, height: 5, borderRadius: 2.5, marginTop: 4 },

  controlRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    marginTop: 16,
    marginBottom: 12,
  },
  summaryTextContainer: { flex: 1 },
  summaryLabel: { fontSize: 12, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.8 },
  progressSubtitle: { fontSize: 18, fontWeight: '700', marginTop: 2 },
  addActionButton: {
    width: 35, 
    height: 35, 
    borderRadius: 8,
    justifyContent: 'center', 
    alignItems: 'center',
    backgroundColor: 'rgba(192, 132, 252, 0.15)', 
    borderWidth: 1,
    borderColor: '#C084FC', 
  },

  scroll: { flex: 1, paddingHorizontal: 20 },
  taskCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    borderRadius: 16,
    borderWidth: 1,
    marginBottom: 10,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 7,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  taskBody: { flex: 1 },
  taskTitle: { fontSize: 15, fontWeight: '600' },
  taskSubject: { fontSize: 13, marginTop: 2 },
  taskTime: { fontSize: 11, fontWeight: '600', marginTop: 4 },
  strikeText: { textDecorationLine: 'line-through', opacity: 0.6 },
  deleteButton: { padding: 8, marginLeft: 6 },

  emptyContainer: { alignItems: 'center', marginTop: 60, width: '100%', paddingHorizontal: 20 },
  emptyText: { marginTop: 12, fontSize: 14, fontWeight: '500', textAlign: 'center' },
  inlineAddBtn: { marginTop: 16, paddingHorizontal: 16, paddingVertical: 8, borderRadius: 10, borderWidth: 1 },

  drawerOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  drawerDismissZone: { flex: 1 },
  drawerSheetContainer: { borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingBottom: 10, maxHeight: '85%' },
  drawerHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 18, borderBottomWidth: 1 },
  drawerTitle: { fontSize: 16, fontWeight: '700' },
  drawerCloseBtn: { padding: 4 },
  drawerBodyContent: { padding: 24 },
  groupHeadingLabel: { fontSize: 11, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.8 },
  textInput: { height: 46, borderRadius: 12, borderWidth: 1, paddingHorizontal: 14, marginTop: 8, fontSize: 14 },
  drawerFooterGroup: { flexDirection: 'row', gap: 12, marginTop: 28, marginBottom: 20 },
  footerBtnSecondary: { flex: 1, height: 46, borderRadius: 12, borderWidth: 1, justifyContent: 'center', alignItems: 'center' },
  footerBtnPrimary: { flex: 2, height: 46, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },

  /* Centered Popup Styles */
  popupOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  popupCardContainer: {
    width: '100%',
    maxWidth: 340,
    borderRadius: 20,
    borderWidth: 1,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 6,
  },
  deleteIconBadge: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: 'rgba(239, 68, 68, 0.12)',
    justifyContent: 'center',
    alignItems: 'center',
  },
});