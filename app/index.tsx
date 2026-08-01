import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import {
    ActivityIndicator,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
    useColorScheme,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './components/footer';
import { Colors } from './constants/theme';

const CONFIG_FILE_URI = `${FileSystem.documentDirectory}config.json`;

interface CompletedRecord {
  score: number;
  maxPossibleScore: number;
  percentage: number;
  completedAt: string;
  questionType?: string;
}

interface PerformanceConfig {
  quizzes?: Record<string, CompletedRecord[]>;
  pastPapers?: Record<string, CompletedRecord[]>;
}

interface ActivityItem {
  lesson: string;
  type: 'Quiz' | 'Past Paper';
  score: number;
  maxPossibleScore: number;
  percentage: number;
  questionType: string;
  completedAt: string;
}

export default function HomeScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [loading, setLoading] = useState(true);
  const [totalCompleted, setTotalCompleted] = useState(0);
  const [overallAverage, setOverallAverage] = useState(0);
  const [typeBreakdown, setTypeBreakdown] = useState<Record<string, number>>({});
  const [recentActivities, setRecentActivities] = useState<ActivityItem[]>([]);

  // Refresh dashboard metrics whenever screen comes into focus
  useFocusEffect(
    useCallback(() => {
      loadDashboardStats();
    }, [])
  );

  const loadDashboardStats = async () => {
    setLoading(true);
    try {
      const configCheck = await FileSystem.getInfoAsync(CONFIG_FILE_URI);
      if (!configCheck.exists) {
        setLoading(false);
        return;
      }

      const rawConfig = await FileSystem.readAsStringAsync(CONFIG_FILE_URI);
      const parsedConfig: PerformanceConfig = JSON.parse(rawConfig);

      let totalCount = 0;
      let percentageSum = 0;
      const typeCounts: Record<string, number> = { mcq: 0, tf: 0, sa: 0, seq: 0 };
      const allActivities: ActivityItem[] = [];

      const processCategory = (
        categoryData: Record<string, CompletedRecord[]> | undefined,
        categoryName: 'Quiz' | 'Past Paper'
      ) => {
        if (!categoryData) return;
        Object.entries(categoryData).forEach(([lessonName, records]) => {
          records.forEach((rec) => {
            totalCount += 1;
            percentageSum += rec.percentage;

            const qType = (rec.questionType || 'mcq').toLowerCase();
            typeCounts[qType] = (typeCounts[qType] || 0) + 1;

            allActivities.push({
              lesson: lessonName,
              type: categoryName,
              score: rec.score,
              maxPossibleScore: rec.maxPossibleScore,
              percentage: rec.percentage,
              questionType: qType,
              completedAt: rec.completedAt,
            });
          });
        });
      };

      processCategory(parsedConfig.quizzes, 'Quiz');
      processCategory(parsedConfig.pastPapers, 'Past Paper');

      // Sort activity by date (Most recent first)
      allActivities.sort((a, b) => new Date(b.completedAt).getTime() - new Date(a.completedAt).getTime());

      setTotalCompleted(totalCount);
      setOverallAverage(totalCount > 0 ? Math.round(percentageSum / totalCount) : 0);
      setTypeBreakdown(typeCounts);
      setRecentActivities(allActivities.slice(0, 5));
    } catch (e) {
      console.warn("Could not load performance stats:", e);
    } finally {
      setLoading(false);
    }
  };

  const getFormatBadgeColor = (type: string) => {
    switch (type.toLowerCase()) {
      case 'mcq': return '#34C759';
      case 'tf': return '#007AFF';
      case 'sa': return '#FF9500';
      case 'seq': return '#AF52DE';
      default: return theme.subtext;
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: theme.border }]}>
        <View>
          <Text style={[styles.headerTitle, { color: theme.text }]}>Dashboard</Text>
        </View>
        <TouchableOpacity style={styles.iconBtn} onPress={loadDashboardStats}>
          <FontAwesome5 name="sync-alt" size={16} color={theme.accent} />
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        {loading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color={theme.accent} />
            <Text style={{ color: theme.subtext, marginTop: 12 }}>Loading Statistics...</Text>
          </View>
        ) : (
          <>
            {/* Top Stat Summary Cards */}
            <View style={styles.statsRow}>
              <View style={[styles.statCard, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}>
                <View style={[styles.iconCircle, { backgroundColor: theme.accent + '20' }]}>
                  <FontAwesome5 name="check-circle" size={18} color={theme.accent} />
                </View>
                <Text style={[styles.statValue, { color: theme.text }]}>{totalCompleted}</Text>
                <Text style={[styles.statLabel, { color: theme.subtext }]}>Completed</Text>
              </View>

              <View style={[styles.statCard, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}>
                <View style={[styles.iconCircle, { backgroundColor: '#34C75920' }]}>
                  <FontAwesome5 name="trophy" size={18} color="#34C759" />
                </View>
                <Text style={[styles.statValue, { color: theme.text }]}>{overallAverage}%</Text>
                <Text style={[styles.statLabel, { color: theme.subtext }]}>Avg Accuracy</Text>
              </View>
            </View>

            {/* Question Type Breakdown */}
            <Text style={[styles.sectionTitle, { color: theme.text }]}>Format Breakdown</Text>
            <View style={[styles.breakdownCard, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}>
              {['mcq', 'tf', 'sa', 'seq'].map((type) => {
                const count = typeBreakdown[type] || 0;
                const percent = totalCompleted > 0 ? Math.round((count / totalCompleted) * 100) : 0;
                const badgeColor = getFormatBadgeColor(type);

                return (
                  <View key={type} style={styles.typeRow}>
                    <View style={styles.typeMeta}>
                      <View style={[styles.typeBadge, { backgroundColor: badgeColor + '20' }]}>
                        <Text style={[styles.typeBadgeText, { color: badgeColor }]}>{type.toUpperCase()}</Text>
                      </View>
                      <Text style={[styles.typeCountText, { color: theme.text }]}>{count} Questions</Text>
                    </View>

                    <View style={styles.progressTrack}>
                      <View style={[styles.progressBar, { width: `${percent}%`, backgroundColor: badgeColor }]} />
                    </View>
                    <Text style={[styles.typePercentText, { color: theme.subtext }]}>{percent}%</Text>
                  </View>
                );
              })}
            </View>

            {/* Recent Activity Log */}
            <Text style={[styles.sectionTitle, { color: theme.text }]}>Recent Attempts</Text>
            {recentActivities.length === 0 ? (
              <View style={[styles.emptyCard, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}>
                <FontAwesome5 name="chart-bar" size={32} color={theme.subtext} />
                <Text style={{ color: theme.subtext, marginTop: 10, textAlign: 'center' }}>
                  No quiz history recorded yet. Complete a quiz or past paper to see your statistics!
                </Text>
              </View>
            ) : (
              recentActivities.map((act, index) => (
                <View
                  key={index}
                  style={[styles.activityCard, { backgroundColor: theme.card ?? theme.buttons, borderColor: theme.border }]}
                >
                  <View style={styles.activityHeader}>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.activityLesson, { color: theme.text }]} numberOfLines={1}>
                        {act.lesson}
                      </Text>
                      <Text style={[styles.activityDate, { color: theme.subtext }]}>
                        {act.type} • {new Date(act.completedAt).toLocaleDateString()}
                      </Text>
                    </View>

                    <View style={styles.activityScoreBadge}>
                      <Text style={[styles.activityScoreText, { color: theme.accent }]}>
                        {act.score}/{act.maxPossibleScore}
                      </Text>
                      <Text style={[styles.activityPercentage, { color: theme.subtext }]}>
                        {act.percentage}%
                      </Text>
                    </View>
                  </View>

                  <View style={styles.activityFooter}>
                    <View style={[styles.typeBadgeSmall, { backgroundColor: getFormatBadgeColor(act.questionType) + '20' }]}>
                      <Text style={[styles.typeBadgeTextSmall, { color: getFormatBadgeColor(act.questionType) }]}>
                        {act.questionType.toUpperCase()}
                      </Text>
                    </View>
                  </View>
                </View>
              ))
            )}
          </>
        )}
      </ScrollView>

      <Footer />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  scroll: { flex: 1, paddingHorizontal: 20 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 14,
    borderBottomWidth: 1,
  },
  headerSubtext: { fontSize: 10, fontWeight: '700', letterSpacing: 1 },
  headerTitle: { fontSize: 24, fontWeight: '700', },
  iconBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingContainer: {
    paddingVertical: 80,
    alignItems: 'center',
    justifyContent: 'center',
  },

  /* Stats Row */
  statsRow: {
    flexDirection: 'row',
    gap: 12,
    marginVertical: 16,
  },
  statCard: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    alignItems: 'flex-start',
  },
  iconCircle: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
  },
  statValue: { fontSize: 22, fontWeight: '800' },
  statLabel: { fontSize: 12, fontWeight: '600', marginTop: 2 },

  sectionTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginTop: 12,
    marginBottom: 10,
  },

  /* Breakdown Section */
  breakdownCard: {
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    gap: 12,
    marginBottom: 10,
  },
  typeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  typeMeta: {
    width: 120,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  typeBadge: {
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 6,
  },
  typeBadgeText: {
    fontSize: 10,
    fontWeight: '800',
  },
  typeCountText: {
    fontSize: 12,
    fontWeight: '600',
  },
  progressTrack: {
    flex: 1,
    height: 8,
    backgroundColor: 'rgba(150, 150, 150, 0.2)',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
    borderRadius: 4,
  },
  typePercentText: {
    width: 35,
    textAlign: 'right',
    fontSize: 12,
    fontWeight: '700',
  },

  /* Recent Activity Cards */
  activityCard: {
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    marginBottom: 10,
  },
  activityHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  activityLesson: {
    fontSize: 14,
    fontWeight: '700',
  },
  activityDate: {
    fontSize: 11,
    marginTop: 2,
  },
  activityScoreBadge: {
    alignItems: 'flex-end',
  },
  activityScoreText: {
    fontSize: 14,
    fontWeight: '800',
  },
  activityPercentage: {
    fontSize: 11,
    fontWeight: '600',
  },
  activityFooter: {
    marginTop: 8,
    flexDirection: 'row',
  },
  typeBadgeSmall: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  typeBadgeTextSmall: {
    fontSize: 9,
    fontWeight: '800',
  },
  emptyCard: {
    padding: 30,
    borderRadius: 16,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});