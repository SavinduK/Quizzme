import { FontAwesome5 } from '@expo/vector-icons';
import React from 'react';
import { Pressable, StyleSheet, Text, useColorScheme, View } from 'react-native';
import { Colors } from '../constants/theme';

interface QuizCardProps {
  item: any;
  chosenAnswer: string | null;
  setChosenAnswer: (ans: string | null) => void;
  setRunningScore: React.Dispatch<React.SetStateAction<number>>;
  runningScore: number; 
  maxPossibleScore: number; 
  tfSelections: Record<number, boolean | null>;
  setTfSelections: React.Dispatch<React.SetStateAction<Record<number, boolean | null>>>;
  tfChecked: boolean;
  tfQuestionScore: number;
  evaluateTfQuestion: () => void;
  handleNextQuestion: () => void;
  currentQuestionIdx: number;
  totalQuestions: number;
}

export default function QuizCard({
  item, chosenAnswer, setChosenAnswer, setRunningScore, runningScore, maxPossibleScore,
  tfSelections, setTfSelections, tfChecked, tfQuestionScore, evaluateTfQuestion,
  handleNextQuestion, currentQuestionIdx, totalQuestions
}: QuizCardProps) {
  const isTFStyle = Array.isArray(item.statements);
  const theme = Colors[useColorScheme() ?? 'light'];

  return (
    <View style={[styles.quizCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
      
      {/* HEADER SECTION */}
      <View style={styles.quizHeaderRow}> 
        <Text style={[styles.progressText, { color: theme.title }]}>
           {currentQuestionIdx + 1} \ {totalQuestions}
        </Text>
        <View style={[styles.scoreBadge, { backgroundColor: theme.accent }]}>
          <Text style={styles.scoreBadgeText}>Score: {runningScore} / {maxPossibleScore}</Text>
        </View>
      </View>

      <Text style={[styles.quizQuestion, { color: theme.title }]}>{item.question}</Text>
      
      {/* STANDARD MCQ */}
      {!isTFStyle && item.options.map((option: string, oIdx: number) => {
        const isSelected = chosenAnswer === option;
        const isCorrect = option === item.correct_answer;
        let optionBg = 'transparent';
        let optionBorder = theme.border;

        if (chosenAnswer) {
          if (isCorrect) { optionBg = 'rgba(74, 222, 128, 0.15)'; optionBorder = '#4ade80'; }
          else if (isSelected) { optionBg = 'rgba(248, 113, 113, 0.15)'; optionBorder = '#f87171'; }
        }

        return (
          <Pressable
            key={oIdx}
            style={[styles.optionButton, { backgroundColor: optionBg, borderColor: optionBorder }]}
            onPress={() => {
              if (chosenAnswer === option) {
                setChosenAnswer(null);
                if (isCorrect) setRunningScore(p => Math.max(0, p - 1));
              } else if (!chosenAnswer) {
                setChosenAnswer(option);
                if (isCorrect) setRunningScore(p => p + 1);
              }
            }}
          >
            <Text style={[styles.optionText, { color: theme.title, fontWeight: isSelected ? '700' : '400' }]}>{option}</Text>
            {chosenAnswer && isCorrect && <FontAwesome5 name="check-circle" size={14} color="#4ade80" />}
            {chosenAnswer && isSelected && !isCorrect && <FontAwesome5 name="times-circle" size={14} color="#f87171" />}
          </Pressable>
        );
      })}

      {/* TRUE / FALSE MULTI-STATEMENT */}
      {isTFStyle && item.statements?.map((statement: string, sIdx: number) => {
        const currentSelection = tfSelections[sIdx];
        const correctBool = item.answers?.[sIdx];
        let statementBorder = theme.border;
        let statementBg = 'transparent';

        if (tfChecked) {
          statementBg = currentSelection === correctBool ? 'rgba(74, 222, 128, 0.08)' : 'rgba(248, 113, 113, 0.08)';
          statementBorder = currentSelection === correctBool ? '#4ade80' : '#f87171';
        }

        return (
          <View key={sIdx} style={[styles.tfStatementRow, { borderColor: statementBorder, backgroundColor: statementBg }]}>
            <View style={{ flex: 1, paddingRight: 4 }}>
              <Text style={[styles.tfStatementText, { color: theme.title }]}>{sIdx + 1}. {statement}</Text>
              {tfChecked && (
                <Text style={[styles.tfFeedbackText, { color: currentSelection === correctBool ? '#4ade80' : '#f87171' }]}>
                  {`(Selected: ${currentSelection === null ? 'None' : (currentSelection ? 'T' : 'F')} | Correct: ${correctBool ? 'T' : 'F'})`}
                </Text>
              )}
            </View>
            <View style={styles.tfActionToggleGroup}>
              <Pressable 
                disabled={tfChecked}
                style={[styles.tfToggleBtn, currentSelection === true && { backgroundColor: theme.accent }]}
                onPress={() => setTfSelections(p => ({ ...p, [sIdx]: currentSelection === true ? null : true }))}
              >
                <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === true ? '#fff' : theme.title }}>T</Text>
              </Pressable>
              <Pressable 
                disabled={tfChecked}
                style={[styles.tfToggleBtn, currentSelection === false && { backgroundColor: theme.delete }]}
                onPress={() => setTfSelections(p => ({ ...p, [sIdx]: currentSelection === false ? null : false }))}
              >
                <Text style={{ fontSize: 11, fontWeight: '700', color: currentSelection === false ? '#fff' : theme.title }}>F</Text>
              </Pressable>
            </View>
          </View>
        );
      })}

      {isTFStyle && tfChecked && (
        <View style={[styles.tfQuestionScoreBadge, { backgroundColor: theme.background }]}>
          <Text style={[styles.tfQuestionScoreText, { color: theme.title }]}>
            Question Marks: <Text style={{ color: theme.accent, fontWeight: '800' }}>+{tfQuestionScore} / 5</Text>
          </Text>
        </View>
      )}

      {isTFStyle && !tfChecked && (
        <Pressable style={[styles.submitActionBtn, { backgroundColor: theme.accent }]} onPress={evaluateTfQuestion}>
          <Text style={styles.submitActionBtnText}>Check Statements</Text>
        </Pressable>
      )}

      {((!isTFStyle && chosenAnswer) || (isTFStyle && tfChecked)) && (
        <Pressable style={[styles.submitActionBtn, { backgroundColor: '#4ade80' }]} onPress={handleNextQuestion}>
          <Text style={styles.submitActionBtnText}>
            {currentQuestionIdx + 1 === totalQuestions ? "View Final Results" : "Next Question"}
          </Text>
          <FontAwesome5 name="arrow-right" size={12} color="white" style={{ marginLeft: 8 }} />
        </Pressable>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
    quizCard: { padding: 20, borderRadius: 22, borderWidth: 1, marginBottom: 16,marginTop:12,},
    quizHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, width: '100%' },
    progressText: { fontSize: 13, fontWeight: '600', opacity: 0.8 },
    scoreBadge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 8 },
    scoreBadgeText: { color: 'white', fontSize: 12, fontWeight: '700' },
    quizQuestion: { fontSize: 16, fontWeight: '700', marginBottom: 18, lineHeight: 22 },
    optionButton: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 14, borderRadius: 14, borderWidth: 1, marginBottom: 8 },
    optionText: { fontSize: 14, flex: 1, paddingRight: 10 },
    tfStatementRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 12, borderRadius: 14, borderWidth: 1, marginBottom: 10, gap: 10 },
    tfStatementText: { fontSize: 13, lineHeight: 18 },
    tfFeedbackText: { fontSize: 11, fontWeight: '600', marginTop: 3 },
    tfActionToggleGroup: { flexDirection: 'row', gap: 6 },
    tfToggleBtn: { width: 28, height: 28, borderRadius: 8, backgroundColor: 'rgba(0,0,0,0.04)', justifyContent: 'center', alignItems: 'center' },
    tfQuestionScoreBadge: { width: '100%', padding: 12, borderRadius: 12, alignItems: 'center', marginTop: 6, marginBottom: 2 },
    tfQuestionScoreText: { fontSize: 13, fontWeight: '600' },
    submitActionBtn: { height: 48, borderRadius: 14, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', marginTop: 14 },
    submitActionBtnText: { color: 'white', fontWeight: '700', fontSize: 14 },
  });